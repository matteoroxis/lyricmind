package it.matteoroxis.lyricmind.vectorizer.model;

import it.matteoroxis.lyricmind.vectorizer.model.dto.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DTOValidationUnitTest {

    @Test
    void songRequest_AllFields_CreatedCorrectly() {
        // Given
        String title = "Test Title";
        String artist = "Test Artist";
        String album = "Test Album";
        String genre = "Rock";
        String mood = "Happy";
        String description = "Test Description";
        String lyrics = "Test Lyrics";
        Integer releaseYear = 2023;

        // When
        SongRequest request = new SongRequest(title, artist, album, genre, mood, description, lyrics, releaseYear);

        // Then
        assertEquals(title, request.title());
        assertEquals(artist, request.artist());
        assertEquals(album, request.album());
        assertEquals(genre, request.genre());
        assertEquals(mood, request.mood());
        assertEquals(description, request.description());
        assertEquals(lyrics, request.lyrics());
        assertEquals(releaseYear, request.releaseYear());
    }

    @Test
    void musicRequest_ValidData_CreatedCorrectly() {
        // Given
        String mood = "energetic";
        Integer limit = 15;

        // When
        MusicRequest request = new MusicRequest(mood, limit);

        // Then
        assertEquals(mood, request.mood());
        assertEquals(limit, request.limit());
    }

    @Test
    void bulkSongRequest_ValidFileName_CreatedCorrectly() {
        // Given
        String fileName = "songs.csv";

        // When
        BulkSongRequest request = new BulkSongRequest(fileName);

        // Then
        assertEquals(fileName, request.fileName());
    }

    @Test
    void bulkSongResponse_ValidCount_CreatedCorrectly() {
        // Given
        Integer count = 42;

        // When
        BulkSongResponse response = new BulkSongResponse(count);

        // Then
        assertEquals(count, response.numberOfSongs());
    }

    @Test
    void songRecommendationResponse_AllFields_CreatedCorrectly() {
        // Given
        String title = "Recommendation Title";
        String artist = "Recommendation Artist";
        String album = "Recommendation Album";
        String genre = "Jazz";
        Integer releaseYear = 2020;
        String motivation = "Perfect for your mood";

        // When
        SongRecommendationResponse response = new SongRecommendationResponse(
                title, artist, album, genre, releaseYear, motivation
        );

        // Then
        assertEquals(title, response.title());
        assertEquals(artist, response.artist());
        assertEquals(album, response.album());
        assertEquals(genre, response.genre());
        assertEquals(releaseYear, response.releaseYear());
        assertEquals(motivation, response.motivation());
    }
}
