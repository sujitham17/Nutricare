package com.nutricare.nutricarebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExcelExportRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String filename;

    @NotNull(message = "Data rows are required")
    private List<Map<String, String>> rows;
}
