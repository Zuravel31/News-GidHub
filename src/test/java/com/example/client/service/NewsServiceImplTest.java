package com.example.client.service;

import com.example.client.client.NewsClient;
import com.example.client.dto.NewsDTO;
import com.example.client.entity.News;
import com.example.client.mapper.NewsMapper;
import com.example.client.repository.NewsRepository;
import net.spy.memcached.MemcachedClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewsServiceImplTest {

    @Mock
    private NewsClient client;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsMapper mapper;

    @Mock
    private MemcachedClient memcachedClient;

    @Spy
    @InjectMocks
    private NewsServiceImpl newsService;

    private NewsDTO newsDTO;
    private News news;


    @BeforeEach
    void setUp() {
        // Инициализация тестовых данных
        newsDTO = new NewsDTO();
        newsDTO.setText("Test news text");
        newsDTO.setKeywords("Test keywords");
        newsDTO.setTime(LocalDateTime.now());

        news = new News();
        news.setText("Test news text");
        news.setKeywords("Test keywords");
        news.setTime(LocalDateTime.now());
        news.setIsSent(false);
    }

//    @Test
//    void fetchAndSaveAllNews_SuccessfulSave() {
//        // Arrange
//        List<NewsDTO> newsDTOs = List.of(newsDTO);
//        String key = "news:" + DigestUtils.md5DigestAsHex(news.getText().getBytes());
//
//        when(client.getAllNews()).thenReturn(newsDTOs);
//        when(mapper.toEntity(newsDTO)).thenReturn(news);
//        when(newsRepository.findByText(news.getText())).thenReturn(Optional.empty());
//        when(memcachedClient.get(key)).thenReturn(null); // Важно, чтобы в кэше не было
//
//        // Act
//        newsService.fetchAndSaveAllNews();
//
//        // Assert
//        verify(newsRepository, times(1)).save(news);
//        verify(memcachedClient, times(1)).set(key, 3600, news); // Проверяем с конкретным ключом
//    }
//
//    @Test
//    void fetchAndSaveAllNews_NewsAlreadyExistsInCache() {
//        // Arrange
//        List<NewsDTO> newsDTOs = List.of(newsDTO);
//        String key = "news:" + DigestUtils.md5DigestAsHex(news.getText().getBytes());
//
//        when(client.getAllNews()).thenReturn(newsDTOs);
//        when(mapper.toEntity(newsDTO)).thenReturn(news);
//        when(memcachedClient.get(key)).thenReturn(news); // Говорим, что в кэше есть
//        when(newsRepository.findByText(news.getText())).thenReturn(Optional.empty()); // Неважно, что в БД
//
//        // Act
//        newsService.fetchAndSaveAllNews();
//
//        // Assert
//        verify(newsRepository, never()).save(any(News.class));
//        verify(memcachedClient, never()).set(anyString(), anyInt(), any(News.class));
//    }


    @Test
    void fetchAndSaveAllNews_NewsAlreadyExistsInDB() {
        // Arrange
        List<NewsDTO> newsDTOs = List.of(newsDTO);

        when(client.getAllNews()).thenReturn(newsDTOs);
        when(mapper.toEntity(newsDTO)).thenReturn(news);
        when(newsRepository.findByText(news.getText())).thenReturn(Optional.of(news));

        // Act
        newsService.fetchAndSaveAllNews();

        // Assert
        verify(newsRepository, never()).save(any(News.class));
        verify(memcachedClient, never()).set(anyString(), anyInt(), any(News.class));
    }

    @Test
    void fetchAndSaveAllNews_MemcachedNotConfigured() {
        // Arrange
        List<NewsDTO> newsDTOs = List.of(newsDTO);

        // Set memcachedClient to null to simulate not configured
        newsService.memcachedClient = null;

        when(client.getAllNews()).thenReturn(newsDTOs);
        when(mapper.toEntity(newsDTO)).thenReturn(news);
        when(newsRepository.findByText(news.getText())).thenReturn(Optional.empty());

        // Act
        newsService.fetchAndSaveAllNews();

        // Assert
        verify(newsRepository, times(1)).save(news);
        // set method should not be called if memcachedClient is null
        verify(memcachedClient, never()).set(anyString(), anyInt(), any(News.class));
    }

    @Test
    void fetchAndSaveAllNews_NoNewsFromClient() {
        // Arrange
        when(client.getAllNews()).thenReturn(null);

        // Act
        newsService.fetchAndSaveAllNews();

        // Assert
        verify(mapper, never()).toEntity(any());
        verify(newsRepository, never()).save(any());
        verify(memcachedClient, never()).set(anyString(), anyInt(), any(News.class));
    }

    @Test
    void checkNews_Success() {
        // Arrange
        // Act
        newsService.checkNews();

        // Assert
        verify(client, times(1)).getAllNews();
    }

    @Test
    void checkNews_Exception() {
        // Arrange
        when(client.getAllNews()).thenThrow(new RuntimeException("Test exception"));

        // Act
        newsService.checkNews();

        // Assert
        // Проверяем, что вызывается логгер с ошибкой
    }

    @Test
    void testCheckNews_Success() {
        doNothing().when(newsService).fetchAndSaveAllNews();

        newsService.checkNews();

        verify(newsService, times(1)).fetchAndSaveAllNews();
    }


    @Test
    void testCheckNews_Exception() {
        doThrow(new RuntimeException("Ошибка при проверке новостей")).when(newsService).fetchAndSaveAllNews();

        newsService.checkNews();

        verify(newsService, times(1)).fetchAndSaveAllNews();
    }
}
