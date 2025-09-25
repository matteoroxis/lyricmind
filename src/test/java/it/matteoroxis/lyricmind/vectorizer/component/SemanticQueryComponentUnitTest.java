package it.matteoroxis.lyricmind.vectorizer.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticQueryComponentUnitTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private SemanticQueryComponent semanticQueryComponent;

    @Test
    void similaritySearch_ValidInput_ReturnsDocuments() {
        // Given
        String mood = "happy";
        int limit = 10;
        List<Document> expectedDocuments = List.of(
                new Document("Test content 1"),
                new Document("Test content 2")
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(expectedDocuments);

        // When
        List<Document> result = semanticQueryComponent.similaritySearch(mood, limit);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedDocuments, result);

//        verify(vectorStore).similaritySearch((SearchRequest) argThat(request ->
//                request.getQuery().contains("happy") &&
//                        request.getTopK() == 20 && // limit * 2
//                        request.getSimilarityThreshold() == 0.6
//        ));
    }

    @Test
    void similaritySearch_EmptyMood_StillBuildsQuery() {
        // Given
        String mood = "";
        int limit = 5;

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        // When
        List<Document> result = semanticQueryComponent.similaritySearch(mood, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void similaritySearch_VectorStoreFailure_PropagatesException() {
        // Given
        String mood = "sad";
        int limit = 10;

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Vector store error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> semanticQueryComponent.similaritySearch(mood, limit));

        assertEquals("Vector store error", exception.getMessage());
    }
}
