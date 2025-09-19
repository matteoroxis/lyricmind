package it.matteoroxis.lyricmind.vectorizer.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.matteoroxis.lyricmind.vectorizer.model.dto.SongRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatasetGeneratorComponent {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(DatasetGeneratorComponent.class);


    public List<SongRequest> generateSongRequestFromCSV(String csvFilePath) throws IOException {
        List<Map<String, Object>> songs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("File CSV vuoto");
            }

            String[] headers = headerLine.split(",");
            Map<String, Integer> columnIndexes = new HashMap<>();

            // Mappa le colonne
            for (int i = 0; i < headers.length; i++) {
                columnIndexes.put(headers[i].trim(), i);
            }

            // Verifica colonne richieste
            String[] requiredColumns = {"Artist", "Title", "Album", "Year", "Date", "Lyric"};
            for (String col : requiredColumns) {
                if (!columnIndexes.containsKey(col)) {
                    throw new IOException("Colonna mancante: " + col);
                }
            }

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] values = parseCsvLine(line);

                    if (values.length >= headers.length) {
                        Map<String, Object> song = new HashMap<>();

                        song.put("artist", getValue(values, columnIndexes, "Artist"));
                        song.put("title", getValue(values, columnIndexes, "Title"));
                        song.put("album", getValue(values, columnIndexes, "Album"));
                        song.put("genre", determineGenre(
                                getValue(values, columnIndexes, "Artist"),
                                getValue(values, columnIndexes, "Title"),
                                getValue(values, columnIndexes, "Lyric")
                        ));

                        // Gestisce l'anno
                        String yearStr = getValue(values, columnIndexes, "Year");
                        try {
                            song.put("releaseYear", Integer.parseInt(yearStr));
                        } catch (NumberFormatException e) {
                            song.put("releaseYear", 1970);
                        }

                        song.put("lyrics", getValue(values, columnIndexes, "Lyric"));

                        songs.add(song);
                    }
                } catch (Exception e) {
                    logger.error("Errore alla riga " + lineNumber + ": " + e.getMessage());
                }
            }
        }
        List<SongRequest> songRequestList = new ArrayList<>();
        for(Map<String, Object> song:songs){
            SongRequest songRequest = new SongRequest(song.get("title")!=null?(String)song.get("title"):"N/A",
                    song.get("artist")!=null?(String) song.get("artist"):"N/A",
                    song.get("album")!=null?(String) song.get("album"):"N/A",
                    song.get("genre")!=null?(String) song.get("genre"):"N/A",
                    "N/A",
                    "N/A",
                    song.get("lyrics")!=null?(String) song.get("lyrics"):"N/A",
                    (Integer)song.get("releaseYear"));

            songRequestList.add(songRequest);
        }

        return songRequestList;
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());

        return values.toArray(new String[0]);
    }

    private String getValue(String[] values, Map<String, Integer> indexes, String column) {
        Integer index = indexes.get(column);
        if (index != null && index < values.length) {
            String value = values[index].trim();
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    private String determineGenre(String artist, String title, String lyrics) {
        String text = (artist + " " + title + " " + lyrics).toLowerCase();

        if (text.contains("rock") || text.contains("guitar")) return "Rock";
        if (text.contains("rap") || text.contains("hip hop")) return "Hip-Hop";
        if (text.contains("country")) return "Country";
        if (text.contains("jazz")) return "Jazz";
        if (text.contains("electronic") || text.contains("techno")) return "Electronic";

        return "Pop"; // Default
    }
}
