package com.example.client.service;

import com.example.client.client.NewsClient;
import com.example.client.dto.NewsDTO;
import com.example.client.entity.News;
import com.example.client.job.BusinessException;
import com.example.client.mapper.NewsMapper;
import com.example.client.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;
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

    private static final String NEWS_PREFIX = "news:";

    private final MemcachedClient memcachedClient;

    private final NewsRepository newsRepository;

    private final NewsMapper mapper;

    private final NewsClient client;

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
                String key = generateKey(news.getText());
                try {
                    News cachedNews = (memcachedClient != null) ? (News) memcachedClient.get(key) : null;
                    Optional<News> existingNews = newsRepository.findByText(news.getText());

                    if (cachedNews != null || existingNews.isPresent()) {
//                        log.warn("Новость уже существует в кэше или базе данных: {}", news.getText());
                        continue;
                    }

                    if (news.getIsSent() == null) {
                        news.setIsSent(false);
                    }

                    try {
                        newsRepository.save(news);
                        log.info("Новость сохранена в базе данных: {}", news.getText());
                        if (memcachedClient != null) {
                            memcachedClient.set(key, 3600, news);
                            log.info("Новость сохранена в Memcached: {}", news.getText());
                        }
                    } catch (DataIntegrityViolationException e) {
                        log.error("Ошибка при сохранении новости в базе данных: {}", news.getText(), e);
                        throw new BusinessException("Ошибка при сохранении новости в БД: " + news.getText(), "DB_SAVE_ERROR", e);
                    }
                } catch (Exception e) {
                    log.error("Ошибка при обработке новости {}: {}", news.getText(), e.getMessage(), e);
                    throw new BusinessException("Ошибка при обработке новости: " + news.getText(), "NEWS_PROCESSING_ERROR", e);
                }
            }
            log.info("Обработано {} новостей.", newsList.size());
        } else {
            log.warn("Не получено новостей от клиента.");
        }
    }

    @Scheduled(fixedRate = 300_000)
    public void checkNews() {
        log.info("Начало метода checkNews");
        try {
            log.info("Запрос новостей у клиента");
            fetchAndSaveAllNews();
            log.info("Завершение метода checkNews");
        } catch (BusinessException e) { // Обрабатываем BusinessException
            log.error("Ошибка при проверке новостей: {} - {}", e.getErrorCode(), e.getMessage(), e);
            // Здесь можно выполнить другие действия, например, отправить уведомление.
        } catch (Exception e) {  // Все остальные исключения
            log.error("Непредвиденная ошибка при проверке новостей", e); // Более общее сообщение
        }
    }
}