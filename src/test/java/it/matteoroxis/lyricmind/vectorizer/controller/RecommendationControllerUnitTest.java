package it.matteoroxis.lyricmind.vectorizer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.matteoroxis.lyricmind.vectorizer.model.dto.MusicRequest;
import it.matteoroxis.lyricmind.vectorizer.model.dto.SongRecommendationResponse;
import it.matteoroxis.lyricmind.vectorizer.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void recommendSongs_ValidRequest_ReturnsRecommendations() throws Exception {
        // Given
        MusicRequest request = new MusicRequest("happy", 5);
        List<SongRecommendationResponse> responses = List.of(
                new SongRecommendationResponse("Test Song", "Test Artist", "Test Album", "Rock", 2023, "Great mood match")
        );

        when(recommendationService.recommendSongs(anyString(), anyInt())).thenReturn(responses);

        // When & Then
        mockMvc.perform(post("/api/lyricmind/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Song"))
                .andExpect(jsonPath("$[0].artist").value("Test Artist"))
                .andExpect(jsonPath("$[0].motivation").value("Great mood match"));
    }

    @Test
    void recommendSongs_NoLimitSpecified_UsesDefault() throws Exception {
        // Given
        MusicRequest request = new MusicRequest("sad", null);
        List<SongRecommendationResponse> responses = List.of();

        when(recommendationService.recommendSongs("sad", 10)).thenReturn(responses);

        // When & Then
        mockMvc.perform(post("/api/lyricmind/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$").isArray())
                .andExpected(jsonPath("$").isEmpty());
    }

    @Test
    void recommendSongs_ServiceException_ReturnsError() throws Exception {
        // Given
        MusicRequest request = new MusicRequest("angry", 3);

        when(recommendationService.recommendSongs(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Recommendation failed"));

        // When & Then
        mockMvc.perform(post("/api/lyricmind/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void recommendSongs_EmptyMood_StillProcesses() throws Exception {
        // Given
        MusicRequest request = new MusicRequest("", 1);
        List<SongRecommendationResponse> responses = List.of();

        when(recommendationService.recommendSongs("", 1)).thenReturn(responses);

        // When & Then
        mockMvc.perform(post("/api/lyricmind/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
