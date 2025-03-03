package com.example.client.client;

import com.example.client.dto.NewsDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Клиент для взаимодействия с API новостей.
 * Используется для получения списка всех новостей с удалённого сервиса.
 */
@FeignClient(name = "messageClient", url = "https://tricky-lies-help.loca.lt/news")
public interface NewsClient {

    /**
     * Получает все новости с удалённого сервиса.
     * Используется аннотация CircuitBreaker для обработки сбоев в случае недоступности сервиса.
     *
     * @return Список объектов {@link NewsDTO}, представляющих все доступные новости.
     */
    @CircuitBreaker(name = "newsClient", fallbackMethod = "fallbackGetAllNews")
    @GetMapping("/all")
    List<NewsDTO> getAllNews();

}