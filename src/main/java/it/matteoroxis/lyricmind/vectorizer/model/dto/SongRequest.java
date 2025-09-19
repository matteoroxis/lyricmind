package it.matteoroxis.lyricmind.vectorizer.model.dto;

public record SongRequest(String title, String artist, String album, String genre, String mood, String description, String lyrics, Integer releaseYear){};
