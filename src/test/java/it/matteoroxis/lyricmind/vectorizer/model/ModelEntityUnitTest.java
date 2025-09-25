package it.matteoroxis.lyricmind.vectorizer.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelEntityUnitTest {

    @Test
    void song_AllFields_SetAndGetCorrectly() {
        // Given
        Song song = new Song();

        // When
        song.setId("test-id");
        song.setTitle("Test Title");
        song.setArtist("Test Artist");
        song.setAlbum("Test Album");
        song.setGenre("Rock");
        song.setLyrics("Test lyrics content");
        song.setDescription("A great song");
        song.setTags(List.of("tag1", "tag2"));
        song.setReleaseYear(2023);

        // Then
        assertEquals("test-id", song.getId());
        assertEquals("Test Title", song.getTitle());
        assertEquals("Test Artist", song.getArtist());
        assertEquals("Test Album", song.getAlbum());
        assertEquals("Rock", song.getGenre());
        assertEquals("Test lyrics content", song.getLyrics());
        assertEquals("A great song", song.getDescription());
        assertEquals(List.of("tag1", "tag2"), song.getTags());
        assertEquals(2023, song.getReleaseYear());
    }

    @Test
    void song_Constructor_SetsCorrectFields() {
        // When
        Song song = new Song("Title", "Artist", "Description");

        // Then
        assertEquals("Title", song.getTitle());
        assertEquals("Artist", song.getArtist());
        assertEquals("Description", song.getDescription());
        assertNull(song.getId());
        assertNull(song.getAlbum());
    }

    @Test
    void songEmbedding_AllFields_SetAndGetCorrectly() {
        // Given
        String id = "embedding-id";
        String songId = "song-id";
        String content = "Embedded content";
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        Map<String, Object> metadata = Map.of("key", "value");

        // When
        SongEmbedding songEmbedding = new SongEmbedding(id, songId, content, embedding, metadata);

        // Then
        assertEquals(id, songEmbedding.getId());
        assertEquals(songId, songEmbedding.getSongId());
        assertEquals(content, songEmbedding.getContent());
        assertEquals(embedding, songEmbedding.getEmbedding());
        assertEquals(metadata, songEmbedding.getMetadata());
    }

    @Test
    void songRecommendation_Constructor_GeneratesMotivation() {
        // Given
        Song song = new Song("Title", "Artist", "Description");
        Map<String, Object> metadata = Map.of("motivation", "Perfect match");
        Double score = 0.95;

        // When
        SongRecommendation recommendation = new SongRecommendation(song, metadata, score);

        // Then
        assertEquals(song, recommendation.getSong());
        assertEquals(metadata, recommendation.getMetadata());
        assertEquals(score, recommendation.getSimilarityScore());
        assertEquals("Perfect match", recommendation.getReasonForRecommendation());
    }

    @Test
    void songRecommendation_SettersAndGetters_WorkCorrectly() {
        // Given
        Map<String, Object> metadata = Map.of("motivation", "data");
        SongRecommendation recommendation = new SongRecommendation(null, metadata, null);
        Song song = new Song();
        Double score = 0.8;
        String reason = "Custom reason";

        // When
        recommendation.setSong(song);
        recommendation.setMetadata(metadata);
        recommendation.setSimilarityScore(score);
        recommendation.setReasonForRecommendation(reason);

        // Then
        assertEquals(song, recommendation.getSong());
        assertEquals(metadata, recommendation.getMetadata());
        assertEquals(score, recommendation.getSimilarityScore());
        assertEquals(reason, recommendation.getReasonForRecommendation());
    }
}