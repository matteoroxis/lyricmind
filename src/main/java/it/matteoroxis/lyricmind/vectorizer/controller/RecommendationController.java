package it.matteoroxis.lyricmind.vectorizer.controller;

import it.matteoroxis.lyricmind.vectorizer.model.SongRecommendation;
import it.matteoroxis.lyricmind.vectorizer.model.dto.MusicRequest;
import it.matteoroxis.lyricmind.vectorizer.model.dto.SongRecommendationResponse;
import it.matteoroxis.lyricmind.vectorizer.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lyricmind/v1/recommendations")
public class RecommendationController {

    @Autowired
    RecommendationService recommendationService;

    Logger logger = LoggerFactory.getLogger(RecommendationController.class);


    @PostMapping
    public ResponseEntity<List<SongRecommendationResponse>> recommendSongs(
            @RequestBody MusicRequest request) {

        List<SongRecommendationResponse> recommendations = recommendationService.recommendSongs(
                request.mood(),
                request.limit() != null ? request.limit() : 10
        );

        return ResponseEntity.ok(recommendations);
    }

}
