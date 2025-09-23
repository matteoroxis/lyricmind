package it.matteoroxis.lyricmind.vectorizer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.matteoroxis.lyricmind.vectorizer.model.dto.BulkSongRequest;
import it.matteoroxis.lyricmind.vectorizer.model.dto.BulkSongResponse;
import it.matteoroxis.lyricmind.vectorizer.service.SongEmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmbeddingsController.class)
class EmbeddingsControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SongEmbeddingService songEmbeddingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createEmbeddingFromBulkSong_ValidRequest_ReturnsCreated() throws Exception {
        // Given
        BulkSongRequest request = new BulkSongRequest("test.csv");
        BulkSongResponse response = new BulkSongResponse(10);

        when(songEmbeddingService.createEmbeddingFromBulkSong(any(BulkSongRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/lyricmind/v1/embeddings/bulk-songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numberOfSongs").value(10));
    }

    @Test
    void createEmbeddingFromBulkSong_ServiceException_ReturnsError() throws Exception {
        // Given
        BulkSongRequest request = new BulkSongRequest("invalid.csv");

        when(songEmbeddingService.createEmbeddingFromBulkSong(any(BulkSongRequest.class)))
                .thenThrow(new RuntimeException("File processing failed"));

        // When & Then
        mockMvc.perform(post("/api/lyricmind/v1/embeddings/bulk-songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createEmbeddingFromBulkSong_InvalidJson_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/lyricmind/v1/embeddings/bulk-songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }
}
