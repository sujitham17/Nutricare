package com.nutricare.nutricarebackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricare.nutricarebackend.dto.DiseaseRequest;
import com.nutricare.nutricarebackend.dto.DiseaseResponse;
import com.nutricare.nutricarebackend.entity.Disease;
import com.nutricare.nutricarebackend.repository.DiseaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiseaseService {

    private final DiseaseRepository diseaseRepository;
    private final ObjectMapper objectMapper;

    public List<DiseaseResponse> getActiveDiseases() {
        return diseaseRepository.findByStatusIgnoreCase("ACTIVE")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<DiseaseResponse> getAllDiseases() {
        return diseaseRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DiseaseResponse getDiseaseById(Long id) {
        Disease disease = diseaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disease not found"));
        return toResponse(disease);
    }

    @Transactional
    public DiseaseResponse createDisease(DiseaseRequest request) {
        if (diseaseRepository.findByNameIgnoreCase(request.getName().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Disease with this name already exists");
        }

        Disease disease = Disease.builder()
                .name(request.getName().trim())
                .overview(request.getOverview())
                .symptoms(serializeList(request.getSymptoms()))
                .recommendedFoods(serializeList(request.getRecommendedFoods()))
                .foodsToAvoid(serializeList(request.getFoodsToAvoid()))
                .nutritionTips(request.getNutritionTips())
                .status(request.getStatus() == null ? "ACTIVE" : request.getStatus())
                .icon(request.getIcon())
                .color(request.getColor())
                .bg(request.getBg())
                .image(request.getImage())
                .description(request.getDescription())
                .build();

        return toResponse(diseaseRepository.save(disease));
    }

    @Transactional
    public DiseaseResponse updateDisease(Long id, DiseaseRequest request) {
        Disease disease = diseaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disease not found"));

        if (request.getName() != null && !request.getName().trim().equalsIgnoreCase(disease.getName())) {
            diseaseRepository.findByNameIgnoreCase(request.getName().trim())
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Disease with this name already exists");
                    });
            disease.setName(request.getName().trim());
        }

        if (request.getOverview() != null) disease.setOverview(request.getOverview());
        if (request.getSymptoms() != null) disease.setSymptoms(serializeList(request.getSymptoms()));
        if (request.getRecommendedFoods() != null) disease.setRecommendedFoods(serializeList(request.getRecommendedFoods()));
        if (request.getFoodsToAvoid() != null) disease.setFoodsToAvoid(serializeList(request.getFoodsToAvoid()));
        if (request.getNutritionTips() != null) disease.setNutritionTips(request.getNutritionTips());
        if (request.getStatus() != null) disease.setStatus(request.getStatus());
        if (request.getIcon() != null) disease.setIcon(request.getIcon());
        if (request.getColor() != null) disease.setColor(request.getColor());
        if (request.getBg() != null) disease.setBg(request.getBg());
        if (request.getImage() != null) disease.setImage(request.getImage());
        if (request.getDescription() != null) disease.setDescription(request.getDescription());

        return toResponse(diseaseRepository.save(disease));
    }

    @Transactional
    public void deleteDisease(Long id) {
        Disease disease = diseaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disease not found"));
        diseaseRepository.delete(disease);
    }

    public DiseaseResponse toResponse(Disease disease) {
        if (disease == null) return null;
        return DiseaseResponse.builder()
                .id(disease.getId())
                .name(disease.getName())
                .overview(disease.getOverview())
                .symptoms(deserializeList(disease.getSymptoms()))
                .recommendedFoods(deserializeList(disease.getRecommendedFoods()))
                .foodsToAvoid(deserializeList(disease.getFoodsToAvoid()))
                .nutritionTips(disease.getNutritionTips())
                .status(disease.getStatus())
                .icon(disease.getIcon())
                .color(disease.getColor())
                .bg(disease.getBg())
                .image(disease.getImage())
                .description(disease.getDescription())
                .build();
    }

    public String serializeList(List<String> list) {
        if (list == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    public List<String> deserializeList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
