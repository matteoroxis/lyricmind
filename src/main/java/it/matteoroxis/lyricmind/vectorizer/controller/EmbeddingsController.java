package it.matteoroxis.lyricmind.vectorizer.controller;

import it.matteoroxis.lyricmind.vectorizer.model.Song;
import it.matteoroxis.lyricmind.vectorizer.model.dto.BulkSongRequest;
import it.matteoroxis.lyricmind.vectorizer.model.dto.BulkSongResponse;
import it.matteoroxis.lyricmind.vectorizer.model.dto.SongRequest;
import it.matteoroxis.lyricmind.vectorizer.component.DatasetGeneratorComponent;
import it.matteoroxis.lyricmind.vectorizer.service.SongEmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lyricmind/v1/embeddings")
public class EmbeddingsController {

    @Autowired
    SongEmbeddingService songEmbeddingService;

    @Autowired
    DatasetGeneratorComponent datasetGeneratorComponent;

    @PostMapping("/songs")
    ResponseEntity<Song> createEmbeddingFromSong(@RequestBody SongRequest request){
        return new ResponseEntity<>(songEmbeddingService.createEmbeddingFromSong(request), HttpStatus.CREATED);
    }

    @PostMapping("/bulk-songs")
    ResponseEntity<BulkSongResponse> createEmbeddingFromBulkSong(@RequestBody BulkSongRequest request){
        return new ResponseEntity<>(songEmbeddingService.createEmbeddingFromBulkSong(request), HttpStatus.CREATED);
    }


}
