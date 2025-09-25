package it.matteoroxis.lyricmind.vectorizer;

import it.matteoroxis.lyricmind.vectorizer.component.DatasetGeneratorComponent;
import it.matteoroxis.lyricmind.vectorizer.model.Song;
import it.matteoroxis.lyricmind.vectorizer.model.dto.SongRequest;
import it.matteoroxis.lyricmind.vectorizer.repository.SongRepository;
import it.matteoroxis.lyricmind.vectorizer.service.SongEmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=test-key",
        "spring.data.mongodb.uri=mongodb://localhost:27017/test"
})
class SongEmbeddingIntegrationTest {

    @Autowired
    private SongEmbeddingService songEmbeddingService;

    @MockBean
    private SongRepository songRepository;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private DatasetGeneratorComponent datasetGeneratorComponent;

    @Test
    void fullEmbeddingFlow_ProcessesCorrectly() {
        // Given
        List<SongRequest> requests = List.of(
                new SongRequest("Song 1", "Artist 1", "Album 1", "Rock", "Happy", "Description", "Lyrics", 2023),
                new SongRequest("Song 2", "Artist 2", "Album 2", "Pop", "Sad", "Description", "Lyrics", 2022)
        );

        Song savedSong1 = createTestSong("1", "Song 1", "Artist 1");
        Song savedSong2 = createTestSong("2", "Song 2", "Artist 2");

        when(songRepository.saveAll(any())).thenReturn(List.of(savedSong1, savedSong2));

        // When
        Integer result = songEmbeddingService.createEmbeddingFromSongList(requests);

        // Then
        assertEquals(2, result);
//        verify(songRepository).saveAll(argThat(songs -> songs. == 2));
//        verify(vectorStore).add(argThat(docs -> docs.size() == 2));
    }

    private Song createTestSong(String id, String title, String artist) {
        Song song = new Song();
        song.setId(id);
        song.setTitle(title);
        song.setArtist(artist);
        song.setAlbum("Test Album");
        song.setGenre("Test Genre");
        song.setLyrics("Test lyrics");
        song.setDescription("Test Description");
        song.setReleaseYear(2023);
        return song;
    }
}