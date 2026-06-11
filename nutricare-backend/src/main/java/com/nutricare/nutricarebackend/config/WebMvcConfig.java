package com.nutricare.nutricarebackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SubscriptionAccessInterceptor subscriptionAccessInterceptor;

    @Value("${nutricare.upload-dir:uploads/profile-images}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(subscriptionAccessInterceptor)
                .addPathPatterns(
                        "/api/appointments/**",
                        "/api/diet-plans/**",
                        "/api/weekly-meal-plans/**",
                        "/api/reports/**",
                        "/api/meal-compliance/**",
                        "/api/chat/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/profile-images/**")
                .addResourceLocations(location);
    }
}
