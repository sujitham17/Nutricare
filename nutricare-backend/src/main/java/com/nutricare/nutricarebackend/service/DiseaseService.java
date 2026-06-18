package com.nutricare.nutricarebackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricare.nutricarebackend.dto.DiseaseRequest;
import com.nutricare.nutricarebackend.dto.DiseaseResponse;
import com.nutricare.nutricarebackend.entity.Disease;
import com.nutricare.nutricarebackend.repository.DiseaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiseaseService {

    private final DiseaseRepository diseaseRepository;
    private final ObjectMapper objectMapper;

    public List<DiseaseResponse> getActiveDiseases() {
        log.info("DiseaseService.getActiveDiseases entered");
        try {
            log.info("Database query started: Fetching active diseases");
            List<Disease> diseases = diseaseRepository.findByStatusIgnoreCase("ACTIVE");
            int recordCount = diseases == null ? 0 : diseases.size();
            log.info("Database query completed. Record count: {}", recordCount);
            if (diseases == null || diseases.isEmpty()) {
                return List.of();
            }
            log.info("DTO mapping started for active diseases");
            return diseases.stream()
                    .map(disease -> {
                        try {
                            return toResponse(disease);
                        } catch (Exception ex) {
                            log.error("Error mapping disease record id={}: {}", disease.getId(), ex.getMessage(), ex);
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Error in getActiveDiseases. Full exception: ", e);
            return List.of();
        }
    }

    public List<DiseaseResponse> getAllDiseases() {
        log.info("DiseaseService.getAllDiseases entered");
        try {
            log.info("Database query started: Fetching all diseases");
            List<Disease> diseases = diseaseRepository.findAll();
            int recordCount = diseases == null ? 0 : diseases.size();
            log.info("Database query completed. Record count: {}", recordCount);
            if (diseases == null || diseases.isEmpty()) {
                return List.of();
            }
            log.info("DTO mapping started for all diseases");
            return diseases.stream()
                    .map(disease -> {
                        try {
                            return toResponse(disease);
                        } catch (Exception ex) {
                            log.error("Error mapping disease record id={}: {}", disease.getId(), ex.getMessage(), ex);
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Error in getAllDiseases. Full exception: ", e);
            return List.of();
        }
    }

    public DiseaseResponse getDiseaseById(Long id) {
        log.info("DiseaseService.getDiseaseById entered. ID: {}", id);
        try {
            log.info("Database query started: Fetching disease by ID: {}", id);
            Disease disease = diseaseRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disease not found"));
            log.info("Database query completed. Disease found: {}", disease.getName());
            log.info("DTO mapping started for disease ID: {}", id);
            return toResponse(disease);
        } catch (ResponseStatusException e) {
            log.error("ResponseStatusException in getDiseaseById id={}: {}", id, e.getReason(), e);
            throw e;
        } catch (Exception e) {
            log.error("Error in getDiseaseById id={}. Full exception: ", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving disease", e);
        }
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
        log.info("Mapping Disease to DTO. Disease ID: {}", disease.getId());
        return DiseaseResponse.builder()
                .id(disease.getId())
                .name(disease.getName())
                .overview(disease.getOverview() != null ? disease.getOverview() : "")
                .symptoms(deserializeList(disease.getSymptoms()))
                .recommendedFoods(deserializeList(disease.getRecommendedFoods()))
                .foodsToAvoid(deserializeList(disease.getFoodsToAvoid()))
                .nutritionTips(disease.getNutritionTips() != null ? disease.getNutritionTips() : "")
                .status(disease.getStatus() != null ? disease.getStatus() : "ACTIVE")
                .icon(disease.getIcon() != null ? disease.getIcon() : "Activity")
                .color(disease.getColor() != null ? disease.getColor() : "#ef4444")
                .bg(disease.getBg() != null ? disease.getBg() : "bg-red-50")
                .image(disease.getImage() != null ? disease.getImage() : "https://images.unsplash.com/photo-1579621970563-ebec7560ff3e?w=400&q=80&auto=format&fit=crop")
                .description(disease.getDescription() != null ? disease.getDescription() : "")
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
        String cleaned = json.trim();
        try {
            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                return objectMapper.readValue(cleaned, new TypeReference<List<String>>() {});
            }
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                try {
                    String parsed = objectMapper.readValue(cleaned, String.class);
                    if (parsed != null) {
                        cleaned = parsed.trim();
                        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                            return objectMapper.readValue(cleaned, new TypeReference<List<String>>() {});
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (!cleaned.isEmpty()) {
                if (cleaned.contains(",")) {
                    return java.util.Arrays.stream(cleaned.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                } else {
                    return List.of(cleaned);
                }
            }
        } catch (Exception e) {
            // fallback to empty list
        }
        return List.of();
    }
}
