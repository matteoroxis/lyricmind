package it.matteoroxis.lyricmind.vectorizer.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class RerankComponent {

    private static final String DEFAULT_MOTIVATION = "Relevant to the requested mood";
    private static final int MAX_RERANK_DOCUMENTS = 50;
    private static final String JSON_WRAPPER_REGEX = "(?s)```json\\s*";
    private static final String MARKDOWN_END_REGEX = "(?s)```";

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    public List<Document> rerank(String mood, List<Document> docs) {

        log.info("Re-ranking {} documents for mood: '{}'", docs.size(), mood);

        try {
            // Limit documents to avoid token limits and improve performance
            List<Document> documentsToRerank = limitDocuments(docs);

            // Create and execute re-ranking prompt
            String prompt = buildRerankingPrompt(mood, documentsToRerank);
            ChatResponse response = executeRerankingQuery(prompt);

            // Parse and process the response
            List<Map<String, Object>> ranking = parseRerankingResponse(response);
            List<Document> rerankedDocs = applyRerankingResults(documentsToRerank, ranking);

            log.info("Successfully re-ranked {} documents (from {} candidates) for mood: '{}'",
                    rerankedDocs.size(), docs.size(), mood);

            return rerankedDocs;

        } catch (Exception e) {
            log.error("Failed to re-rank documents for mood: '{}'", mood, e);
            throw new RuntimeException("Document re-ranking failed", e);
        }
    }

    private List<Document> limitDocuments(List<Document> docs) {
        if (docs.size() <= MAX_RERANK_DOCUMENTS) {
            return docs;
        }

        log.info("Limiting documents from {} to {} for re-ranking", docs.size(), MAX_RERANK_DOCUMENTS);
        return docs.subList(0, MAX_RERANK_DOCUMENTS);
    }


    private String buildRerankingPrompt(String mood, List<Document> docs) {
        StringBuilder documentsText = new StringBuilder();

        IntStream.range(0, docs.size())
                .forEach(i -> {
                    Document doc = docs.get(i);
                    String artist = extractMetadata(doc, "artist");
                    String title = extractMetadata(doc, "title");
                    String genre = extractMetadata(doc, "genre");

                    documentsText.append("Doc ").append(i + 1).append(": ")
                            .append("Artist: ").append(artist).append(", ")
                            .append("Title: ").append(title);

                    if (StringUtils.hasText(genre)) {
                        documentsText.append(", Genre: ").append(genre);
                    }

                    documentsText.append("\n");
                });

        return String.format("""
                You are a music recommendation ranking assistant.
                
                Rank the following songs based on their semantic relevance to the requested mood.
                Consider the artist, title, genre, and overall musical style when determining relevance.
                Provide a brief motivation for each ranking without referencing other songs.
                
                Requested Mood: %s
                
                Songs to rank:
                %s
                
                Instructions:
                - Return ONLY a JSON array
                - Include ALL documents in your response
                - Sort by relevance (most relevant first)
                - Score should be between 0.0 and 1.0
                - Keep motivations concise (max 100 characters)
                
                Expected format:
                [{"doc_index": 1, "score": 0.95, "motivation": "Upbeat tempo matches energetic mood"}]
                """,
                sanitizeInput(mood), documentsText);
    }


    private ChatResponse executeRerankingQuery(String prompt) {
        try {
            log.debug("Executing re-ranking query with prompt length: {} characters", prompt.length());

            Prompt aiPrompt = new Prompt(new UserMessage(prompt));
            ChatResponse response = chatModel.call(aiPrompt);

            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new RuntimeException("Invalid response from AI model");
            }

            return response;

        } catch (Exception e) {
            log.error("Failed to execute re-ranking query", e);
            throw new RuntimeException("AI model query failed", e);
        }
    }

    private List<Map<String, Object>> parseRerankingResponse(ChatResponse response) {
        try {
            String content = response.getResult().getOutput().getText();
            log.debug("Received re-ranking response: {}", content);

            String cleanedJson = cleanJsonResponse(content);

            List<Map<String, Object>> ranking = objectMapper.readValue(
                    cleanedJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            validateRankingResponse(ranking);
            return ranking;

        } catch (IOException e) {
            log.error("Failed to parse re-ranking JSON response", e);
            throw new RuntimeException("Invalid JSON response from AI model", e);
        }
    }

    private String cleanJsonResponse(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            throw new RuntimeException("Empty response from AI model");
        }

        return rawResponse
                .replaceAll(JSON_WRAPPER_REGEX, "")
                .replaceAll(MARKDOWN_END_REGEX, "")
                .trim();
    }


    private void validateRankingResponse(List<Map<String, Object>> ranking) {
        if (ranking == null || ranking.isEmpty()) {
            throw new RuntimeException("Empty ranking response from AI model");
        }

        for (Map<String, Object> item : ranking) {
            if (!item.containsKey("doc_index") || !item.containsKey("motivation")) {
                throw new RuntimeException("Invalid ranking item structure: missing required fields");
            }
        }
    }


    private List<Document> applyRerankingResults(List<Document> originalDocs, List<Map<String, Object>> ranking) {
        List<Document> rerankedDocs = new ArrayList<>();
        int processedCount = 0;

        for (Map<String, Object> item : ranking) {
            try {
                int index = extractDocumentIndex(item);
                String motivation = extractMotivation(item);

                if (isValidDocumentIndex(index, originalDocs.size())) {
                    Document doc = originalDocs.get(index);
                    addMotivationMetadata(doc, motivation);
                    rerankedDocs.add(doc);
                    processedCount++;
                } else {
                    log.warn("Invalid document index {} for document list of size {}",
                            index + 1, originalDocs.size());
                }

            } catch (Exception e) {
                log.warn("Failed to process ranking item: {}", item, e);
            }
        }
        return rerankedDocs;
    }


    private int extractDocumentIndex(Map<String, Object> rankingItem) {
        Object docIndexObj = rankingItem.get("doc_index");
        if (docIndexObj instanceof Number number) {
            return number.intValue() - 1; // Convert to zero-based index
        }
        throw new RuntimeException("Invalid doc_index type: " + docIndexObj.getClass());
    }


    private String extractMotivation(Map<String, Object> rankingItem) {
        Object motivationObj = rankingItem.get("motivation");
        return motivationObj != null ? motivationObj.toString().trim() : DEFAULT_MOTIVATION;
    }


    private boolean isValidDocumentIndex(int index, int listSize) {
        return index >= 0 && index < listSize;
    }


    private void addMotivationMetadata(Document document, String motivation) {
        document.getMetadata().put("motivation", motivation);
    }


    private String extractMetadata(Document document, String key) {
        Object value = document.getMetadata().get(key);
        return value != null ? value.toString().trim() : "Unknown";
    }

    private String sanitizeInput(String input) {
        return input.replaceAll("[\"'`]", "").trim();
    }
}