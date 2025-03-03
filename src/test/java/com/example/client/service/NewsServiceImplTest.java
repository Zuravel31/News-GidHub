package com.example.client.service;

import com.example.client.client.NewsClient;
import com.example.client.dto.NewsDTO;
import com.example.client.entity.News;
import com.example.client.job.BusinessException;
import com.example.client.mapper.NewsMapper;
import com.example.client.repository.NewsRepository;
import net.spy.memcached.MemcachedClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Nested
@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsMapper newsMapper;

    @Mock
    private NewsClient newsClient;

    @Mock
    private MemcachedClient memcachedClient;

    @Spy
    @InjectMocks
    private NewsServiceImpl newsService;

    @Test
    public void testFetchAndSaveAllNews_SuccessfulSave() {
        // Arrange
        NewsDTO newsDTO = new NewsDTO();
        newsDTO.setText("Test News");
        List<NewsDTO> newsDTOs = new ArrayList<>();
        newsDTOs.add(newsDTO);

        News news = new News();
        news.setText("Test News");

        when(newsClient.getAllNews()).thenReturn(newsDTOs);
        when(newsMapper.toEntity(newsDTO)).thenReturn(news);
        when(newsRepository.findByText("Test News")).thenReturn(Optional.empty());
        when(memcachedClient.get(anyString())).thenReturn(null);
        // Ensure memcachedClient is not null (if you can't remove @Autowired(required=false))
        //Mockito.doReturn(memcachedClient).when(newsService).memcachedClient;  // This might not work directl
        // Act
        newsService.fetchAndSaveAllNews();
        // Assert
        verify(newsRepository, times(1)).save(news);
        verify(memcachedClient, times(1)).set(anyString(), eq(3600), eq(news));
    }

    @Test
    public void testFetchAndSaveAllNews_DuplicateNews() {
        // Arrange
        NewsDTO newsDTO = new NewsDTO();
        newsDTO.setText("Duplicate News");
        List<NewsDTO> newsDTOs = new ArrayList<>();
        newsDTOs.add(newsDTO);

        News news = new News();
        news.setText("Duplicate News");

        when(newsClient.getAllNews()).thenReturn(newsDTOs);
        when(newsMapper.toEntity(newsDTO)).thenReturn(news);
        when(newsRepository.findByText("Duplicate News")).thenReturn(Optional.of(news)); // News already exists
        // Act
        newsService.fetchAndSaveAllNews();
        // Assert
        verify(newsRepository, never()).save(any());
        verify(memcachedClient, never()).set(anyString(), anyInt(), any());
    }

    @Test
    public void testFetchAndSaveAllNews_DatabaseError() {
        // Arrange
        NewsDTO newsDTO = new NewsDTO();
        newsDTO.setText("Problematic News");
        List<NewsDTO> newsDTOs = new ArrayList<>();
        newsDTOs.add(newsDTO);

        News news = new News();
        news.setText("Problematic News");

        when(newsClient.getAllNews()).thenReturn(newsDTOs);
        when(newsMapper.toEntity(newsDTO)).thenReturn(news);
        when(newsRepository.findByText("Problematic News")).thenReturn(Optional.empty());
        when(newsRepository.save(news)).thenThrow(new DataIntegrityViolationException("Simulated DB error"));

        // Act & Assert
        assertThrows(BusinessException.class, () -> newsService.fetchAndSaveAllNews());

        // Assert (verify logging - depends on your logging framework)
        // You can use a logging appender to capture log messages and assert on them.
    }

    @Test
    public void testCheckNews_Successful() {
        // Arrange
        doNothing().when(newsService).fetchAndSaveAllNews(); // Mock the internal call

        // Act
        newsService.checkNews();

        // Assert
        verify(newsService, times(1)).fetchAndSaveAllNews();
    }

    @Test
    public void testCheckNews_BusinessException() {
        // Arrange
        doThrow(new BusinessException("Simulated business problem", "TEST_ERROR"))
                .when(newsService).fetchAndSaveAllNews();

        // Act
        newsService.checkNews();

        // Assert (verify logging - depends on your logging framework)
        // You can use a logging appender to capture log messages and assert on them.
    }

    @Test
    public void testCheckNews_GenericException() {
        // Arrange
        doThrow(new RuntimeException("Simulated runtime problem"))
                .when(newsService).fetchAndSaveAllNews();

        // Act
        newsService.checkNews();

        // Assert (verify logging - depends on your logging framework)
        // You can use a logging appender to capture log messages and assert on them.
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