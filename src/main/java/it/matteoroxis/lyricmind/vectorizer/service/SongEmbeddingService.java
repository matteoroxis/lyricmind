package it.matteoroxis.lyricmind.vectorizer.service;

import it.matteoroxis.lyricmind.vectorizer.component.DatasetGeneratorComponent;
import it.matteoroxis.lyricmind.vectorizer.model.Song;
import it.matteoroxis.lyricmind.vectorizer.model.dto.BulkSongRequest;
import it.matteoroxis.lyricmind.vectorizer.model.dto.BulkSongResponse;
import it.matteoroxis.lyricmind.vectorizer.model.dto.SongRequest;
import it.matteoroxis.lyricmind.vectorizer.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongEmbeddingService {

    private static final String RESOURCES_PATH = "src/main/resources/";

    private final SongRepository songRepository;
    private final VectorStore vectorStore;
    private final DatasetGeneratorComponent datasetGeneratorComponent;

    /**
     * Creates and embeds multiple songs from a list of requests.
     *
     * @param requestList the list of song requests
     * @return the number of successfully embedded songs
     * @throws IllegalArgumentException if requestList is null or empty
     */
    @Transactional
    public Integer createEmbeddingFromSongList(List<SongRequest> requestList) {
        if (requestList == null || requestList.isEmpty()) {
            throw new IllegalArgumentException("Song request list cannot be null or empty");
        }

        log.info("Starting bulk embedding for {} songs", requestList.size());

        List<Song> savedSongs = new ArrayList<>();
        List<Document> documents = new ArrayList<>();

        try {
            // Process and save songs in batch
            for (SongRequest request : requestList) {
                Song song = mapRequestToSong(request);
                savedSongs.add(song);
            }

            // Save all songs to database
            savedSongs = songRepository.saveAll(savedSongs);

            // Create documents for embedding
            documents = savedSongs.stream()
                    .map(this::createDocumentFromSong)
                    .collect(Collectors.toList());

            // Embed all documents
            embedDocuments(documents);

            log.info("Successfully embedded {} songs", documents.size());
            return documents.size();

        } catch (Exception e) {
            log.error("Failed to embed songs in bulk", e);
            throw new RuntimeException("Bulk embedding failed", e);
        }
    }

    /**
     * Creates and embeds songs from a CSV file.
     *
     * @param request the bulk song request containing the filename
     * @return response with the number of embedded songs
     * @throws RuntimeException if file processing fails
     */
    @Transactional
    public BulkSongResponse createEmbeddingFromBulkSong(BulkSongRequest request) {
        if (request == null || request.fileName() == null || request.fileName().trim().isEmpty()) {
            throw new IllegalArgumentException("Bulk request and filename cannot be null or empty");
        }

        String filePath = Paths.get(RESOURCES_PATH, request.fileName()).toString();
        log.info("Processing bulk song embedding from file: {}", filePath);

        try {
            List<SongRequest> songRequestList = datasetGeneratorComponent.generateSongRequestFromCSV(filePath);

            if (songRequestList.isEmpty()) {
                log.warn("No songs found in file: {}", filePath);
                return new BulkSongResponse(0);
            }

            Integer numberOfEmbeddedSongs = createEmbeddingFromSongList(songRequestList);

            log.info("Successfully processed {} songs from file: {}", numberOfEmbeddedSongs, request.fileName());
            return new BulkSongResponse(numberOfEmbeddedSongs);

        } catch (IOException e) {
            log.error("Failed to read CSV file: {}", filePath, e);
            throw new RuntimeException("Failed to process CSV file: " + request.fileName(), e);
        }
    }

    /**
     * Creates a Document from a Song entity for vector embedding.
     *
     * @param song the song entity
     * @return the document ready for embedding
     */
    private Document createDocumentFromSong(Song song) {
        if (song == null) {
            throw new IllegalArgumentException("Song cannot be null");
        }

        StringBuilder content = new StringBuilder();
        content.append("Title: ").append(sanitizeText(song.getTitle())).append("\n");
        content.append("Artist: ").append(sanitizeText(song.getArtist())).append("\n");
        content.append("Lyrics: ").append(sanitizeText(song.getLyrics())).append("\n");

        Map<String, Object> metadata = createMetadata(song);

        return new Document(content.toString(), metadata);
    }

    /**
     * Creates metadata map from song entity.
     *
     * @param song the song entity
     * @return metadata map
     */
    private Map<String, Object> createMetadata(Song song) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("songId", song.getId());
        metadata.put("title", song.getTitle());
        metadata.put("artist", song.getArtist());
        metadata.put("album", song.getAlbum());
        metadata.put("genre", song.getGenre());
        metadata.put("description", song.getDescription());
        metadata.put("releaseYear", song.getReleaseYear());
        return metadata;
    }

    /**
     * Embeds a list of documents in the vector store.
     *
     * @param documents the documents to embed
     */
    private void embedDocuments(List<Document> documents) {
        try {
            vectorStore.add(documents);
            log.debug("Successfully embedded {} documents", documents.size());
        } catch (Exception e) {
            log.error("Failed to embed documents", e);
            throw new RuntimeException("Vector embedding failed", e);
        }
    }


    /**
     * Maps a SongRequest to a Song entity.
     *
     * @param request the song request
     * @return the mapped song entity
     */
    private Song mapRequestToSong(SongRequest request) {
        Song song = new Song();
        song.setTitle(sanitizeText(request.title()));
        song.setArtist(sanitizeText(request.artist()));
        song.setAlbum(sanitizeText(request.album()));
        song.setGenre(sanitizeText(request.genre()));
        song.setDescription(sanitizeText(request.description()));
        song.setLyrics(sanitizeText(request.lyrics()));
        song.setReleaseYear(request.releaseYear());
        return song;
    }

    /**
     * Sanitizes text by trimming and handling null values.
     *
     * @param text the text to sanitize
     * @return sanitized text or empty string if null
     */
    private String sanitizeText(String text) {
        return text != null ? text.trim() : "";
    }
}