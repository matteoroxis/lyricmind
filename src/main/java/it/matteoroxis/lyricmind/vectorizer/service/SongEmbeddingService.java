package it.matteoroxis.lyricmind.vectorizer.service;

import it.matteoroxis.lyricmind.vectorizer.component.DatasetGeneratorComponent;
import it.matteoroxis.lyricmind.vectorizer.model.Song;
import it.matteoroxis.lyricmind.vectorizer.model.dto.BulkSongRequest;
import it.matteoroxis.lyricmind.vectorizer.model.dto.BulkSongResponse;
import it.matteoroxis.lyricmind.vectorizer.model.dto.SongRequest;
import it.matteoroxis.lyricmind.vectorizer.repository.SongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class SongEmbeddingService {

    private final SongRepository songRepository;
    private final VectorStore vectorStore;
    private final DatasetGeneratorComponent datasetGeneratorComponent;
    private Logger logger = LoggerFactory.getLogger(SongEmbeddingService.class);

    public SongEmbeddingService(SongRepository songRepository,
                                VectorStore vectorStore, DatasetGeneratorComponent datasetGeneratorComponent) {
        this.songRepository = songRepository;
        this.vectorStore = vectorStore;
        this.datasetGeneratorComponent = datasetGeneratorComponent;
    }


    public Song createEmbeddingFromSong(SongRequest request) {

        Song song = createSongFromSongRequest(request);
        song = songRepository.save(song);

        Document songDocument = createDocumentFromSong(song);
        embedSongDocument(songDocument);

        logger.info("Embedded Song: {},{}",song.getArtist(), song.getTitle());

        return song;

    }

    public Integer createEmbeddingFromSongList(List<SongRequest> requestList) {

List<Document> songDocumentList = new ArrayList<>();

        for(SongRequest request:requestList){
            Song song = createSongFromSongRequest(request);
            song = songRepository.save(song);

            Document songDocument = createDocumentFromSong(song);
            songDocumentList.add(songDocument);

            logger.info("Embedding Song: {},{}",song.getArtist(), song.getTitle());

        }
        embedSongDocumentList(songDocumentList);

        logger.info("Embedded {} Song",songDocumentList.size());

        return songDocumentList.size();
    }

    private Document createDocumentFromSong(Song song) {
   StringBuilder content= new StringBuilder();

   content.append("Title: ").append(song.getTitle()).append("\n");
        content.append("Artist: ").append(song.getArtist()).append("\n");
        content.append("Lyrics: ").append(song.getLyrics()).append("\n");

        Map<String,Object> metadata = new HashMap<>();
        metadata.put("songId",song.getId());
        metadata.put("title",song.getTitle());
        metadata.put("artist",song.getArtist());
        metadata.put("album",song.getAlbum());
        metadata.put("genre", song.getGenre());
        metadata.put("description",song.getDescription());
        metadata.put("releaseYear",song.getReleaseYear());

        return new Document(content.toString(),metadata);

    }

    public void embedSongDocumentList(List<Document> songDocumentList) {
        vectorStore.add(songDocumentList);
    }

    public void embedSongDocument(Document songDocument) {
        vectorStore.add(Arrays.asList(songDocument));
    }


    private Song createSongFromSongRequest(SongRequest request) {

        Song song = new Song();
        song.setArtist(request.artist());
        song.setAlbum(request.album());
        song.setDescription(request.description());
        song.setGenre(request.genre());
        song.setReleaseYear(request.releaseYear());
        song.setTitle(request.title());
        song.setLyrics(request.lyrics());

        return song;
    }

    public BulkSongResponse createEmbeddingFromBulkSong(BulkSongRequest request) {

        List<SongRequest> songRequestList = new ArrayList<>();
        try {
            songRequestList = datasetGeneratorComponent.generateSongRequestFromCSV("src/main/resources/"+request.fileName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Integer numberOfEmbeddedSongs = createEmbeddingFromSongList(songRequestList);

        return new BulkSongResponse(numberOfEmbeddedSongs);
    }
}