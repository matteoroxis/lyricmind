package it.matteoroxis.lyricmind.vectorizer.service;

import it.matteoroxis.lyricmind.vectorizer.component.RerankComponent;
import it.matteoroxis.lyricmind.vectorizer.component.SemanticQueryComponent;
import it.matteoroxis.lyricmind.vectorizer.model.Song;
import it.matteoroxis.lyricmind.vectorizer.model.dto.SongRecommendationResponse;
import it.matteoroxis.lyricmind.vectorizer.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private final SongRepository songRepository;
    private final RerankComponent rerankComponent;
    private final SemanticQueryComponent semanticQueryComponent;

    public List<SongRecommendationResponse> recommendSongs(String mood, int limit) {

        log.info("Requesting song recommendations for mood: '{}' with limit: {}", mood, limit);

        try {
            // Get candidate songs through semantic search
            List<Document> candidates = findCandidateSongs(mood, limit);

            if (candidates.isEmpty()) {
                log.info("No candidate songs found for mood: '{}'", mood);
                return Collections.emptyList();
            }
            // Re-rank candidates using AI
            List<Document> rerankedResults = rerankCandidates(mood, candidates);
            // Map to recommendation responses
            List<SongRecommendationResponse> recommendations = mapDocumentsToRecommendations(rerankedResults, limit);

            log.info("Successfully generated {} recommendations for mood: '{}'", recommendations.size(), mood);
            return recommendations;

        } catch (Exception e) {
            log.error("Failed to generate recommendations for mood: '{}'", mood, e);
            throw new RuntimeException("Recommendation generation failed", e);
        }
    }

    private List<Document> findCandidateSongs(String mood, int limit) {
        try {
            List<Document> candidates = semanticQueryComponent.similaritySearch(mood, limit);
            return candidates;

        } catch (Exception e) {
            log.error("Failed to find candidate songs for mood: '{}'", mood, e);
            throw new RuntimeException("Candidate search failed", e);
        }
    }

    private List<Document> rerankCandidates(String mood, List<Document> candidates) {
        try {
            List<Document> rerankedResults = rerankComponent.rerank(mood, candidates);
            return rerankedResults;
        } catch (Exception e) {
            log.error("Failed to re-rank candidates for mood: '{}'", mood, e);
            return candidates;
        }
    }


    private List<SongRecommendationResponse> mapDocumentsToRecommendations(List<Document> documents, int limit) {
        return documents.stream()
                .limit(limit)
                .map(this::mapDocumentToRecommendation)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<SongRecommendationResponse> mapDocumentToRecommendation(Document document) {
        try {

            String songId = extractSongId(document);
            if (!StringUtils.hasText(songId)) {
                log.warn("Song ID is missing or empty in document metadata");
                return Optional.empty();
            }

            Optional<Song> songOptional = findSongById(songId);
            if (songOptional.isEmpty()) {
                log.warn("Song not found for ID: {}", songId);
                return Optional.empty();
            }

            Song song = songOptional.get();
            String motivation = extractMotivation(document);

            SongRecommendationResponse recommendation = createRecommendationResponse(song, motivation);

            log.debug("Successfully mapped song: '{}' by '{}' to recommendation",
                    song.getTitle(), song.getArtist());

            return Optional.of(recommendation);

        } catch (Exception e) {
            log.error("Failed to map document to recommendation", e);
            return Optional.empty();
        }
    }


    private String extractSongId(Document document) {
        Object songIdObj = document.getMetadata().get("songId");
        return songIdObj != null ? songIdObj.toString() : null;
    }


    private String extractMotivation(Document document) {
        Object motivationObj = document.getMetadata().get("motivation");
        return motivationObj != null ? motivationObj.toString() : "Recommended based on mood similarity";
    }


    private Optional<Song> findSongById(String songId) {
        try {
            return songRepository.findById(songId);
        } catch (Exception e) {
            log.error("Database error while finding song with ID: {}", songId, e);
            return Optional.empty();
        }
    }

    private SongRecommendationResponse createRecommendationResponse(Song song, String motivation) {
        return new SongRecommendationResponse(
                sanitizeText(song.getTitle()),
                sanitizeText(song.getArtist()),
                sanitizeText(song.getAlbum()),
                sanitizeText(song.getGenre()),
                song.getReleaseYear(),
                sanitizeText(motivation)
        );
    }

    private String sanitizeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : "";
    }
}