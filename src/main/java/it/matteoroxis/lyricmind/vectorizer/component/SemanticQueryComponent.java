package it.matteoroxis.lyricmind.vectorizer.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SemanticQueryComponent {

    private final VectorStore vectorStore;
    private Logger logger = LoggerFactory.getLogger(SemanticQueryComponent.class);

    public SemanticQueryComponent(VectorStore vectorStore){
        this.vectorStore = vectorStore;
    }


    public List<Document> similaritySearch(String mood, int limit) {
        String query = buildSemanticQuery(mood);

        logger.info("Building semantic query: "+query);

        // Ricerca semantica nel vector store
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(limit*2)
                .similarityThreshold(0.6)
                .build();

         return vectorStore.similaritySearch(searchRequest);

    }

    private String buildSemanticQuery(String mood) {
        return String.format(
                "Mood: %s. " +
                        "Search for songs that match this mood.",
                mood
        );
    }
}
