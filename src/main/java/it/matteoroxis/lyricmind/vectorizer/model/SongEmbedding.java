package it.matteoroxis.lyricmind.vectorizer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "song_embedding")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongEmbedding {

    @Id
    private String id;
    private String songId;
    private String content;
    private List<Double> embedding;
    private Map<String,Object> metadata;

}
