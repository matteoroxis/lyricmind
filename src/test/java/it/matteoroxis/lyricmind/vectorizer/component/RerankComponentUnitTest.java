package it.matteoroxis.lyricmind.vectorizer.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RerankComponentUnitTest {

    @org.mockito.Mock
    OpenAiChatModel chatModel;

    // Use a real ObjectMapper for JSON parsing
    ObjectMapper objectMapper = new ObjectMapper();

    RerankComponent component;

    @Captor
    ArgumentCaptor<Prompt> promptCaptor;

    @BeforeEach
    void setUp() {
        component = new RerankComponent(chatModel, objectMapper);
    }

    // Helper: create a Document with minimal metadata
    private Document doc(String artist, String title, String genre) {
        Map<String, Object> md = new HashMap<>();
        md.put("artist", artist);
        md.put("title", title);
        if (genre != null) md.put("genre", genre);
        return new Document("lyrics...", md);
    }

    // Helper: mock the AI model to return the given raw text
    private void mockAiReturns(String rawText) {
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        Message message = new AssistantMessage(rawText);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn((AssistantMessage) message);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
    }

    @Test
    void rerank_happyPath_ordersAndAddsMotivations() {
        // Given
        List<Document> docs = List.of(
                doc("Artist A", "Title A", "Pop"),
                doc("Artist B", "Title B", "Rock")
        );

        // Model ranks doc #2 first, then doc #1 (indices are 1-based in the response)
        String json = "[{\"doc_index\":2,\"score\":0.91,\"motivation\":\"Highly consistent with the mood\"}," +
                " {\"doc_index\":1,\"score\":0.35,\"motivation\":\"Partially consistent\"}]";
        mockAiReturns(json);

        // When
        List<Document> ranked = component.rerank("energetic", docs);

        // Then
        assertEquals(2, ranked.size());
        assertEquals("Title B", ranked.get(0).getMetadata().get("title")); // doc #2 first
        assertEquals("Title A", ranked.get(1).getMetadata().get("title")); // then doc #1

        // Motivation should be injected into metadata
        assertEquals("Highly consistent with the mood", ranked.get(0).getMetadata().get("motivation"));
        assertEquals("Partially consistent", ranked.get(1).getMetadata().get("motivation"));

        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void rerank_cleansMarkdownCodeFence() {
        // Given
        List<Document> docs = List.of(
                doc("Artist A", "Title A", null),
                doc("Artist B", "Title B", null)
        );

        String fenced =
                "```json\n" +
                        "[{\"doc_index\":2,\"score\":0.9,\"motivation\":\"OK\"}," +
                        " {\"doc_index\":1,\"score\":0.4,\"motivation\":\"OK\"}]\n" +
                        "```";
        mockAiReturns(fenced);

        // When
        List<Document> ranked = component.rerank("calm", docs);

        // Then
        assertEquals(2, ranked.size());
        assertEquals("Title B", ranked.get(0).getMetadata().get("title"));
        assertEquals("OK", ranked.get(0).getMetadata().get("motivation"));
    }

    @Test
    void rerank_limitsTo50Documents_and_sanitizesMoodInPrompt() {
        // Given: 60 docs (only the first 50 should be included in the prompt)
        List<Document> docs = IntStream.rangeClosed(1, 60)
                .mapToObj(i -> doc("Artist " + i, "Title " + i, (i % 2 == 0 ? "Pop" : "Rock")))
                .collect(Collectors.toList());

        // Ranking references only docs within the first 50
        String json = "[{\"doc_index\":50,\"score\":0.99,\"motivation\":\"Top\"}," +
                " {\"doc_index\":1,\"score\":0.5,\"motivation\":\"Base\"}]";
        mockAiReturns(json);

        // Mood contains characters that sanitizeInput removes:  \" '  `
        String rawMood = "mo\"o'd `X";

        // When
        component.rerank(rawMood, docs);

        // Then: capture the Prompt to inspect its content
        verify(chatModel).call(promptCaptor.capture());
        Prompt usedPrompt = promptCaptor.getValue();

        // The Prompt is built with a single UserMessage
        Message msg = usedPrompt.getInstructions().get(0);
        assertTrue(msg instanceof UserMessage);
        String promptText = msg.getText();

        // Only docs 1..50 must be present
        assertTrue(promptText.contains("Doc 50:"), "Prompt must contain 'Doc 50:'");
        assertFalse(promptText.contains("Doc 51:"), "Prompt must NOT contain 'Doc 51:'");
        assertFalse(promptText.contains("Doc 60:"), "Prompt must NOT contain 'Doc 60:'");

        // Mood should be sanitized: "mo\"o'd `X" -> "mood X"
        assertTrue(promptText.contains("Requested Mood: mood X"),
                "Sanitized mood should appear in the prompt");
    }

    @Test
    void rerank_invalidModelResponse_throws() {
        // Given: model returns ChatResponse with null result
        ChatResponse bad = mock(ChatResponse.class);
        when(bad.getResult()).thenReturn(null);
        when(chatModel.call(any(Prompt.class))).thenReturn(bad);

        List<Document> docs = List.of(doc("A", "T", null));

        // When + Then
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> component.rerank("any", docs));
        assertTrue(ex.getMessage().contains("Document re-ranking failed"));
    }

    @Test
    void rerank_rankingItemMissingRequiredFields_throws() {
        // Given: item without motivation or without doc_index
        String invalidJson = "[{\"doc_index\":1},{\"motivation\":\"missing index\"}]";
        mockAiReturns(invalidJson);

        List<Document> docs = List.of(doc("A", "T", null));

        // When + Then (internal validation should fail)
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> component.rerank("any", docs));
        assertTrue(ex.getMessage().contains("Document re-ranking failed"));
    }

    @Test
    void rerank_outOfRangeIndex_isSkipped() {
        // Given: ranking with an index far beyond the list size
        String json = "[{\"doc_index\":999,\"score\":0.1,\"motivation\":\"out\"}]";
        mockAiReturns(json);

        List<Document> docs = List.of(
                doc("A", "T1", null),
                doc("B", "T2", null)
        );

        // When
        List<Document> ranked = component.rerank("any", docs);

        // Then: no valid documents processed
        assertTrue(ranked.isEmpty());
    }

    @Test
    void rerank_nonNumericDocIndex_itemSkipped() {
        // Given: doc_index is a string -> extractDocumentIndex throws, item is skipped
        String json = "[{\"doc_index\":\"one\",\"motivation\":\"test\"}]";
        mockAiReturns(json);

        List<Document> docs = List.of(doc("A", "T", null));

        // When
        List<Document> ranked = component.rerank("any", docs);

        // Then
        assertTrue(ranked.isEmpty());
    }
}