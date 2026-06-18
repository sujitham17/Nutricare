package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.DiseaseResponse;
import com.nutricare.nutricarebackend.service.DiseaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/diseases")
@RequiredArgsConstructor
@Slf4j
public class DiseaseController {

    private final DiseaseService diseaseService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DiseaseResponse>> getActiveDiseases() {
        log.info("Entering getActiveDiseases");
        try {
            log.info("Query started: Fetching active diseases");
            List<DiseaseResponse> diseases = diseaseService.getActiveDiseases();
            log.info("Query completed. Record count: {}", diseases.size());
            return ResponseEntity.ok(diseases);
        } catch (Exception e) {
            log.error("Exception in getActiveDiseases: {}", e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DiseaseResponse> getDiseaseById(@PathVariable Long id) {
        log.info("Entering getDiseaseById. ID: {}", id);
        try {
            log.info("Query started: Fetching disease by ID: {}", id);
            DiseaseResponse disease = diseaseService.getDiseaseById(id);
            log.info("Query completed for getDiseaseById. Disease found: {}", disease != null ? disease.getName() : "null");
            return ResponseEntity.ok(disease);
        } catch (ResponseStatusException e) {
            log.error("ResponseStatusException in getDiseaseById id={}: {}", id, e.getReason(), e);
            throw e;
        } catch (Exception e) {
            log.error("Exception in getDiseaseById id={}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Disease not found");
        }
    }
}
