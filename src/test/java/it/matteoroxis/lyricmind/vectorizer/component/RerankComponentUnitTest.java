package it.matteoroxis.lyricmind.vectorizer.component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RerankComponentUnitTest {

    @Mock
    private OpenAiChatModel chatModel;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RerankComponent rerankComponent;

    private List<Document> testDocuments;
    private ChatResponse mockResponse;

    @BeforeEach
    void setUp() {
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("artist", "Beatles");
        metadata1.put("title", "Hey Jude");
        metadata1.put("genre", "Rock");

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("artist", "Queen");
        metadata2.put("title", "Bohemian Rhapsody");
        metadata2.put("genre", "Rock");

        testDocuments = List.of(
                new Document("Content 1", metadata1),
                new Document("Content 2", metadata2)
        );

        mockResponse = mock(ChatResponse.class);
        // Setup di base per il mock - solo quello che serve per tutti i test
        lenient().when(mockResponse.getResult()).thenReturn(mock(org.springframework.ai.chat.model.Generation.class));
        lenient().when(mockResponse.getResult().getOutput()).thenReturn(mock(org.springframework.ai.chat.messages.AssistantMessage.class));
    }

    @Test
    void rerank_ValidInput_ReturnsRankedDocuments() throws Exception {
        // Given
        String mood = "happy";
        String jsonResponse = """
            [
                {"doc_index": 1, "score": 0.9, "motivation": "Uplifting melody"},
                {"doc_index": 2, "score": 0.8, "motivation": "Epic and inspiring"}
            ]
            """;

        when(mockResponse.getResult().getOutput().getText()).thenReturn(jsonResponse);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        List<Map<String, Object>> ranking = List.of(
                Map.of("doc_index", 1, "score", 0.9, "motivation", "Uplifting melody"),
                Map.of("doc_index", 2, "score", 0.8, "motivation", "Epic and inspiring")
        );
        when(objectMapper.readValue(eq(jsonResponse), any(TypeReference.class))).thenReturn(ranking);

        // When
        List<Document> result = rerankComponent.rerank(mood, testDocuments);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Uplifting melody", result.get(0).getMetadata().get("motivation"));
        assertEquals("Epic and inspiring", result.get(1).getMetadata().get("motivation"));

        verify(chatModel).call(any(Prompt.class));
        verify(objectMapper).readValue(eq(jsonResponse), any(TypeReference.class));
    }

    @Test
    void rerank_EmptyDocuments_ThrowsException() {
        // Given
        String mood = "happy";
        List<Document> emptyDocs = List.of();

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> rerankComponent.rerank(mood, emptyDocs));

        assertEquals("Document re-ranking failed", exception.getMessage());
    }

    @Test
    void rerank_AIModelFailure_ThrowsException() {
        // Given
        String mood = "happy";
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("AI Error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> rerankComponent.rerank(mood, testDocuments));

        assertEquals("Document re-ranking failed", exception.getMessage());
    }

    @Test
    void rerank_InvalidJsonResponse_ThrowsException() throws Exception {
        // Given
        String mood = "happy";
        String invalidJson = "invalid json";

        when(mockResponse.getResult().getOutput().getText()).thenReturn(invalidJson);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);
        when(objectMapper.readValue(eq(invalidJson), any(TypeReference.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Invalid JSON"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> rerankComponent.rerank(mood, testDocuments));

        assertEquals("Document re-ranking failed", exception.getMessage());
    }

    @Test
    void rerank_LimitedDocuments_ProcessesCorrectly() throws Exception {
        // Given
        String mood = "energetic";
        // Create more than 50 documents to test limiting
        List<Document> manyDocuments = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("artist", "Artist" + i);
            metadata.put("title", "Title" + i);
            manyDocuments.add(new Document("Content" + i, metadata));
        }

        String jsonResponse = "[]";
        when(mockResponse.getResult().getOutput().getText()).thenReturn(jsonResponse);
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);
        when(objectMapper.readValue(eq(jsonResponse), any(TypeReference.class))).thenReturn(List.of());

        // When
        List<Document> result = rerankComponent.rerank(mood, manyDocuments);
    }
}
