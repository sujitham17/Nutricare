package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.DiseaseResponse;
import com.nutricare.nutricarebackend.service.DiseaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/diseases")
@RequiredArgsConstructor
public class DiseaseController {

    private final DiseaseService diseaseService;

    @GetMapping
    public ResponseEntity<List<DiseaseResponse>> getActiveDiseases() {
        return ResponseEntity.ok(diseaseService.getActiveDiseases());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiseaseResponse> getDiseaseById(@PathVariable Long id) {
        return ResponseEntity.ok(diseaseService.getDiseaseById(id));
    }
}
