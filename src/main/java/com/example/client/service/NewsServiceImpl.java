package com.example.client.service;

import com.example.client.client.NewsClient;
import com.example.client.dto.NewsDTO;
import com.example.client.entity.News;
import com.example.client.mapper.NewsMapper;
import com.example.client.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsMapper mapper;

    private final NewsRepository newsRepository;

    private final NewsClient client;

    private static final String NEWS_PREFIX = "news:";
    @Autowired(required = false) // Memcached может быть не настроен
    MemcachedClient memcachedClient;

    // Метод для генерации ключа на основе MD5 хеша
    private String generateKey(String text) {
        String hash = DigestUtils.md5DigestAsHex(text.getBytes());
        return NEWS_PREFIX + hash;
    }

    public void fetchAndSaveAllNews() {
        List<NewsDTO> newsDTOs = client.getAllNews();
        if (newsDTOs != null && !newsDTOs.isEmpty()) {
            List<News> newsList = newsDTOs.stream()
                    .map(mapper::toEntity)
                    .toList();

            for (News news : newsList) {
                // Генерируем ключ на основе MD5 хеша
                String key = generateKey(news.getText());
                try {
                    // 1. Проверяем Memcached
                    News cachedNews = (memcachedClient != null) ? (News) memcachedClient.get(key) : null;
                    // 2. Проверяем базу данных, если нет в Memcached
                    Optional<News> existingNews = newsRepository.findByText(news.getText());
                    // 3. Если новость есть в Memcached ИЛИ в базе данных, пропускаем
                    if (cachedNews != null || existingNews.isPresent()) {
//                        log.info("Новость найдена в Memcached или базе данных, пропущена: {}", news.getText());
                        continue; // Переходим к следующей новости
                    }
                    // 4. Установка значения для isSent, если оно не установлено
                    if (news.getIsSent() == null) {
                        news.setIsSent(false); // или true, в зависимости от логики
                    }
                    // 5. Сохранение новости
                    try {
                        newsRepository.save(news);
                        log.info("Новость сохранена в базе данных: {}", news.getText());

                        // 6. Кэшируем новость в Memcached после сохранения в базу данных
                        if (memcachedClient != null) {
                            memcachedClient.set(key, 3600, news); // Кэшируем на 1 час
                            log.info("Новость сохранена в Memcached: {}", news.getText());
                        }
                    } catch (DataIntegrityViolationException e) {
                        log.error("Ошибка при сохранении новости в базе данных: {}", news.getText(), e);
                    }
                } catch (Exception e) {
                    log.error("Ошибка при обработке новости {}: {}", news.getText(), e.getMessage(), e);
                }
            }
            log.info("Обработано {} новостей.", newsList.size());
        } else {
            log.warn("Не получено новостей от клиента.");
        }
    }


    @Scheduled(fixedRate = 300_000)  // Запускать каждые 5 минут
    public void checkNews() {
        log.info("Начало метода checkNews");

        try {
            log.info("Запрос новостей у клиента");
            fetchAndSaveAllNews();
            log.info("Завершение метода checkNews");
        } catch (Exception e) {
            log.error("Ошибка при проверке новостей", e);
        }
    }
}