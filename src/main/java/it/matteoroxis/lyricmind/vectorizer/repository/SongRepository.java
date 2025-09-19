package it.matteoroxis.lyricmind.vectorizer.repository;

import it.matteoroxis.lyricmind.vectorizer.model.Song;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepository extends MongoRepository<Song, String> {
}