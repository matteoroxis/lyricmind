package it.matteoroxis.lyricmind.vectorizer.service;

import it.matteoroxis.lyricmind.vectorizer.component.DatasetGeneratorComponent;
import it.matteoroxis.lyricmind.vectorizer.model.Song;
import it.matteoroxis.lyricmind.vectorizer.model.dto.BulkSongRequest;
import it.matteoroxis.lyricmind.vectorizer.model.dto.BulkSongResponse;
import it.matteoroxis.lyricmind.vectorizer.model.dto.SongRequest;
import it.matteoroxis.lyricmind.vectorizer.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongEmbeddingServiceUnitTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DatasetGeneratorComponent datasetGeneratorComponent;

    @InjectMocks
    private SongEmbeddingService songEmbeddingService;

    private SongRequest testSongRequest;
    private Song testSong;

    @BeforeEach
    void setUp() {
        testSongRequest = new SongRequest(
                "Test Song",
                "Test Artist",
                "Test Album",
                "Rock",
                "Happy",
                "A great song",
                "Test lyrics",
                2023
        );

        testSong = new Song();
        testSong.setId("song123");
        testSong.setTitle("Test Song");
        testSong.setArtist("Test Artist");
        testSong.setAlbum("Test Album");
        testSong.setGenre("Rock");
        testSong.setDescription("A great song");
        testSong.setLyrics("Test lyrics");
        testSong.setReleaseYear(2023);
    }

    @Test
    void createEmbeddingFromSongList_ValidInput_ReturnsCorrectCount() {
        // Given
        List<SongRequest> requestList = List.of(testSongRequest);
        List<Song> savedSongs = List.of(testSong);

        when(songRepository.saveAll(anyList())).thenReturn(savedSongs);

        // When
        Integer result = songEmbeddingService.createEmbeddingFromSongList(requestList);

        // Then
        assertEquals(1, result);
        verify(songRepository).saveAll(anyList());
        verify(vectorStore).add(anyList());
    }

    @Test
    void createEmbeddingFromSongList_EmptyList_ThrowsException() {
        // Given
        List<SongRequest> emptyList = List.of();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> songEmbeddingService.createEmbeddingFromSongList(emptyList));

        assertEquals("Song request list cannot be null or empty", exception.getMessage());
    }

    @Test
    void createEmbeddingFromSongList_NullList_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> songEmbeddingService.createEmbeddingFromSongList(null));

        assertEquals("Song request list cannot be null or empty", exception.getMessage());
    }

    @Test
    void createEmbeddingFromBulkSong_ValidRequest_ReturnsCorrectResponse() throws IOException {
        // Given
        BulkSongRequest request = new BulkSongRequest("test.csv");
        List<SongRequest> songRequests = List.of(testSongRequest);
        List<Song> savedSongs = List.of(testSong);

        when(datasetGeneratorComponent.generateSongRequestFromCSV(anyString())).thenReturn(songRequests);
        when(songRepository.saveAll(anyList())).thenReturn(savedSongs);

        // When
        BulkSongResponse result = songEmbeddingService.createEmbeddingFromBulkSong(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.numberOfSongs());
        verify(datasetGeneratorComponent).generateSongRequestFromCSV(contains("test.csv"));
        verify(songRepository).saveAll(anyList());
        verify(vectorStore).add(anyList());
    }

    @Test
    void createEmbeddingFromBulkSong_NullRequest_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> songEmbeddingService.createEmbeddingFromBulkSong(null));

        assertEquals("Bulk request and filename cannot be null or empty", exception.getMessage());
    }

    @Test
    void createEmbeddingFromBulkSong_EmptyFileName_ThrowsException() {
        // Given
        BulkSongRequest request = new BulkSongRequest("");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> songEmbeddingService.createEmbeddingFromBulkSong(request));

        assertEquals("Bulk request and filename cannot be null or empty", exception.getMessage());
    }

    @Test
    void createEmbeddingFromBulkSong_FileNotFound_ThrowsException() throws IOException {
        // Given
        BulkSongRequest request = new BulkSongRequest("nonexistent.csv");

        when(datasetGeneratorComponent.generateSongRequestFromCSV(anyString()))
                .thenThrow(new IOException("File not found"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> songEmbeddingService.createEmbeddingFromBulkSong(request));

        assertTrue(exception.getMessage().contains("Failed to process CSV file"));
    }

    @Test
    void createEmbeddingFromBulkSong_EmptyCsv_ReturnsZero() throws IOException {
        // Given
        BulkSongRequest request = new BulkSongRequest("empty.csv");

        when(datasetGeneratorComponent.generateSongRequestFromCSV(anyString())).thenReturn(List.of());

        // When
        BulkSongResponse result = songEmbeddingService.createEmbeddingFromBulkSong(request);

        // Then
        assertNotNull(result);
        assertEquals(0, result.numberOfSongs());
        verifyNoInteractions(songRepository);
        verifyNoInteractions(vectorStore);
    }

    @Test
    void createEmbeddingFromSongList_DatabaseFailure_ThrowsException() {
        // Given
        List<SongRequest> requestList = List.of(testSongRequest);

        when(songRepository.saveAll(anyList())).thenThrow(new RuntimeException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> songEmbeddingService.createEmbeddingFromSongList(requestList));

        assertEquals("Bulk embedding failed", exception.getMessage());
    }

    @Test
    void createEmbeddingFromSongList_VectorStoreFailure_ThrowsException() {
        // Given
        List<SongRequest> requestList = List.of(testSongRequest);
        List<Song> savedSongs = List.of(testSong);

        when(songRepository.saveAll(anyList())).thenReturn(savedSongs);
        doThrow(new RuntimeException("Vector store error")).when(vectorStore).add(anyList());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> songEmbeddingService.createEmbeddingFromSongList(requestList));

        assertEquals("Bulk embedding failed", exception.getMessage());
    }
}