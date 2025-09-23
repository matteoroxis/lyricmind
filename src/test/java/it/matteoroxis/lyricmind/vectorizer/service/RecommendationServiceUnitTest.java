package it.matteoroxis.lyricmind.vectorizer.service;

import it.matteoroxis.lyricmind.vectorizer.component.RerankComponent;
import it.matteoroxis.lyricmind.vectorizer.component.SemanticQueryComponent;
import it.matteoroxis.lyricmind.vectorizer.model.Song;
import it.matteoroxis.lyricmind.vectorizer.model.dto.SongRecommendationResponse;
import it.matteoroxis.lyricmind.vectorizer.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceUnitTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private RerankComponent rerankComponent;

    @Mock
    private SemanticQueryComponent semanticQueryComponent;

    @InjectMocks
    private RecommendationService recommendationService;

    private Song testSong;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        testSong = new Song();
        testSong.setId("song123");
        testSong.setTitle("Test Song");
        testSong.setArtist("Test Artist");
        testSong.setAlbum("Test Album");
        testSong.setGenre("Rock");
        testSong.setReleaseYear(2023);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("songId", "song123");
        metadata.put("motivation", "Perfect match for your mood");

        testDocument = new Document("Test content", metadata);
    }

    @Test
    void recommendSongs_ValidInput_ReturnsRecommendations() {
        // Given
        String mood = "happy";
        int limit = 5;

        List<Document> candidates = List.of(testDocument);
        List<Document> reranked = List.of(testDocument);

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(candidates);
        when(rerankComponent.rerank(mood, candidates)).thenReturn(reranked);
        when(songRepository.findById("song123")).thenReturn(Optional.of(testSong));

        // When
        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        SongRecommendationResponse response = result.get(0);
        assertEquals("Test Song", response.title());
        assertEquals("Test Artist", response.artist());
        assertEquals("Test Album", response.album());
        assertEquals("Rock", response.genre());
        assertEquals(2023, response.releaseYear());
        assertEquals("Perfect match for your mood", response.motivation());

        verify(semanticQueryComponent).similaritySearch(mood, limit);
        verify(rerankComponent).rerank(mood, candidates);
        verify(songRepository).findById("song123");
    }

    @Test
    void recommendSongs_NoCandidatesFound_ReturnsEmptyList() {
        // Given
        String mood = "unknown";
        int limit = 5;

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(List.of());

        // When
        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(semanticQueryComponent).similaritySearch(mood, limit);
        verifyNoInteractions(rerankComponent);
    }

    @Test
    void recommendSongs_RerankFailure_FallbackToCandidates() {
        // Given
        String mood = "happy";
        int limit = 5;

        List<Document> candidates = List.of(testDocument);

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(candidates);
        when(rerankComponent.rerank(mood, candidates)).thenThrow(new RuntimeException("Rerank failed"));
        when(songRepository.findById("song123")).thenReturn(Optional.of(testSong));

        // When
        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(semanticQueryComponent).similaritySearch(mood, limit);
        verify(rerankComponent).rerank(mood, candidates);
        verify(songRepository).findById("song123");
    }

    @Test
    void recommendSongs_SongNotFound_FiltersOut() {
        // Given
        String mood = "happy";
        int limit = 5;

        List<Document> candidates = List.of(testDocument);

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(candidates);
        when(rerankComponent.rerank(mood, candidates)).thenReturn(candidates);
        when(songRepository.findById("song123")).thenReturn(Optional.empty());

        // When
        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void recommendSongs_SemanticSearchFailure_ThrowsException() {
        // Given
        String mood = "happy";
        int limit = 5;

        when(semanticQueryComponent.similaritySearch(mood, limit))
                .thenThrow(new RuntimeException("Search failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> recommendationService.recommendSongs(mood, limit));

        assertEquals("Recommendation generation failed", exception.getMessage());
    }

    @Test
    void recommendSongs_MissingSongId_FiltersOut() {
        // Given
        String mood = "happy";
        int limit = 5;

        Map<String, Object> metadataWithoutSongId = new HashMap<>();
        metadataWithoutSongId.put("motivation", "Test motivation");
        Document docWithoutSongId = new Document("Content", metadataWithoutSongId);

        List<Document> candidates = List.of(docWithoutSongId);

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(candidates);
        when(rerankComponent.rerank(mood, candidates)).thenReturn(candidates);

        // When
        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(songRepository);
    }
}