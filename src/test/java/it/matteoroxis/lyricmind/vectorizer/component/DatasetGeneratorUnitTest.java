package it.matteoroxis.lyricmind.vectorizer.component;

import it.matteoroxis.lyricmind.vectorizer.model.dto.SongRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DatasetGeneratorUnitTest {

    @InjectMocks
    private DatasetGeneratorComponent datasetGeneratorComponent;

    @TempDir
    Path tempDir;

    private String validCsvContent;
    private String csvFilePath;

    @BeforeEach
    void setUp() throws IOException {
        validCsvContent = """
                Artist,Title,Album,Year,Date,Lyric
                "The Beatles","Hey Jude","1968","1968","1968-08-26","Hey Jude, don't make it bad"
                "Queen","Bohemian Rhapsody","A Night at the Opera","1975","1975-10-31","Is this the real life?"
                """;

        csvFilePath = tempDir.resolve("test.csv").toString();
        try (FileWriter writer = new FileWriter(csvFilePath)) {
            writer.write(validCsvContent);
        }
    }

    @Test
    void generateSongRequestFromCSV_ValidFile_ReturnsCorrectSongs() throws IOException {
        // When
        List<SongRequest> result = datasetGeneratorComponent.generateSongRequestFromCSV(csvFilePath);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        SongRequest firstSong = result.get(0);
        assertEquals("Hey Jude", firstSong.title());
        assertEquals("The Beatles", firstSong.artist());
        assertEquals("1968", firstSong.album());
        assertEquals(1968, firstSong.releaseYear());
        assertEquals("Hey Jude, don't make it bad", firstSong.lyrics());
    }

    @Test
    void generateSongRequestFromCSV_EmptyFile_ThrowsIOException() throws IOException {
        // Given
        String emptyFile = tempDir.resolve("empty.csv").toString();
        try (FileWriter writer = new FileWriter(emptyFile)) {
            writer.write("");
        }

        // When & Then
        IOException exception = assertThrows(IOException.class,
                () -> datasetGeneratorComponent.generateSongRequestFromCSV(emptyFile));
        assertEquals("File CSV vuoto", exception.getMessage());
    }

    @Test
    void generateSongRequestFromCSV_MissingColumn_ThrowsIOException() throws IOException {
        // Given
        String invalidContent = "Artist,Title,Album\nBeatles,Hey Jude,1968";
        String invalidFile = tempDir.resolve("invalid.csv").toString();
        try (FileWriter writer = new FileWriter(invalidFile)) {
            writer.write(invalidContent);
        }

        // When & Then
        IOException exception = assertThrows(IOException.class,
                () -> datasetGeneratorComponent.generateSongRequestFromCSV(invalidFile));
        assertTrue(exception.getMessage().startsWith("Colonna mancante:"));
    }

    @Test
    void generateSongRequestFromCSV_FileNotExists_ThrowsIOException() {
        // Given
        String nonExistentFile = "non_existent.csv";

        // When & Then
        assertThrows(IOException.class,
                () -> datasetGeneratorComponent.generateSongRequestFromCSV(nonExistentFile));
    }

}