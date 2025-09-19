package it.matteoroxis.lyricmind.vectorizer.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RerankComponent {

    @Autowired
    ObjectMapper objectMapper;

    private final OpenAiChatModel chatModel;
    private Logger logger = LoggerFactory.getLogger(RerankComponent.class);


    public RerankComponent(OpenAiChatModel chatModel){
        this.chatModel = chatModel;
    }


    public List<Document> rerank(String mood,  List<Document> docs) {
        String prompt = createPrompt(mood, docs);
        var response = chatModel.call(new Prompt(new UserMessage(prompt)));
        List<Map<String, Object>> ranking = parseResponseFromChatModel(response);
        return createListOfRerankedDocument(docs, ranking);
    }

    private List<Document> createListOfRerankedDocument(List<Document> docs, List<Map<String, Object>> ranking) {
        List<Document> rerankedDocs = new ArrayList<>();
        for (Map<String, Object> item : ranking) {
            int index = ((Number) item.get("doc_index")).intValue() - 1;
            String motivation = item.get("motivation").toString();
            if (index >= 0 && index < docs.size()) {
                docs.get(index).getMetadata().put("motivation",motivation);
                rerankedDocs.add(docs.get(index));
            }
        }
        return rerankedDocs;
    }

    private List<Map<String, Object>> parseResponseFromChatModel(ChatResponse response) {
        // Parsing JSON
        String content = response.getResult().getOutput().getText();

        logger.info(content);

        // Clean Markdown wrapper (```json ... ```)
        String json = content
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```", "")
                .trim();

        List<Map<String, Object>> ranking;
        try {
            ranking = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Error while parsing reranking JSON", e);
        }
        return ranking;
    }

    private String createPrompt(String mood, List<Document> docs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            sb.append("Doc ").append(i+1).append(": ")
                    .append(docs.get(i).getMetadata().get("artist"))
                    .append("\n")
                    .append(docs.get(i).getMetadata().get("title"))
                    .append("\n");
        }

        String prompt = """
        You are a ranking assistant.
        Rank the following documents based on their semantic relevance to the query, including the lyrics of the song, and give the motivation of the relevance, without reference to other songs or documents.

        Mood: %s
        Documents:
        %s

        Return JSON: [{"doc_index": i, "score": relevance_score, "motivation": motivation}]
        """.formatted(mood, sb);

        return prompt;
    }

}
