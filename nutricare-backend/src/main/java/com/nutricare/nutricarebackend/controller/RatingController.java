package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.RatingRequest;
import com.nutricare.nutricarebackend.dto.RatingResponse;
import com.nutricare.nutricarebackend.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<RatingResponse> createRating(
            Authentication authentication,
            @Valid @RequestBody RatingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ratingService.createRating(authentication.getName(), request));
    }

    @GetMapping("/dietician/{dieticianId}")
    public ResponseEntity<List<RatingResponse>> getDieticianRatings(@PathVariable Long dieticianId) {
        return ResponseEntity.ok(ratingService.getDieticianRatings(dieticianId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<RatingResponse>> getMyRatings(Authentication authentication) {
        return ResponseEntity.ok(ratingService.getMyRatings(authentication.getName()));
    }
}
