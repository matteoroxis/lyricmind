package it.matteoroxis.lyricmind.vectorizer.service;

import it.matteoroxis.lyricmind.vectorizer.component.RerankComponent;
import it.matteoroxis.lyricmind.vectorizer.component.SemanticQueryComponent;
import it.matteoroxis.lyricmind.vectorizer.model.Song;
import it.matteoroxis.lyricmind.vectorizer.model.SongRecommendation;
import it.matteoroxis.lyricmind.vectorizer.model.dto.SongRecommendationResponse;
import it.matteoroxis.lyricmind.vectorizer.repository.SongRepository;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final SongRepository songRepository;
    private final RerankComponent rerankComponent;
    private final SemanticQueryComponent semanticQueryComponent;
    private Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    public RecommendationService(SongRepository songRepository, RerankComponent rerankComponent, SemanticQueryComponent semanticQueryComponent) {
        this.songRepository = songRepository;
        this.rerankComponent = rerankComponent;
        this.semanticQueryComponent = semanticQueryComponent;
    }

    public List<SongRecommendationResponse> recommendSongs(String mood, int limit) {

        List<Document> candidates = semanticQueryComponent.similaritySearch(mood, limit);

        //Re-rank con GPT
        List<Document> results =  rerankComponent.rerank(mood,candidates);

        return results.stream()
                .limit(limit)
                .map(this::mapDocumentToRecommendation)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }



    private SongRecommendationResponse mapDocumentToRecommendation(Document document) {
        try {
            String songId = (String) document.getMetadata().get("songId");
            Song song = songRepository.findById(songId).orElse(null);

            if (song != null) {

                return new SongRecommendationResponse(song.getTitle(),song.getArtist(),song.getAlbum(),song.getGenre(),song.getReleaseYear(),document.getMetadata().get("motivation").toString());

//                return new SongRecommendation(
//                        song,
//                        document.getMetadata(),
//                        (Double) document.getMetadata().get("distance")
//                );
            }
        } catch (Exception e) {
            // Log error
            logger.error("Exception while retrieving document for recommendation: {}",e);
        }
        return null;
    }
}
