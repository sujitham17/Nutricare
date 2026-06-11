package com.nutricare.nutricarebackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricare.nutricarebackend.dto.ApiErrorResponse;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import com.nutricare.nutricarebackend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(forbiddenAccessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register/send-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register/verify-otp").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/api/profile/**").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/ratings").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/ratings/my").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/ratings/dietician/**").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/dieticians").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dieticians/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/subscription-plans").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/subscription-plans/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/diseases").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/diseases/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/dietician/profile").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.POST, "/api/appointments/book").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/appointments/my").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/appointments/dietician").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.GET, "/api/appointments/dietician/pending").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.PUT, "/api/appointments/*/status").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.PUT, "/api/appointments/*/approve").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.PUT, "/api/appointments/*/reschedule").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.PUT, "/api/appointments/*/cancel").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/appointments/*/meeting").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.PUT, "/api/appointments/*/rating").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/appointments/*/consultation-notes").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.POST, "/api/diet-plans").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.GET, "/api/diet-plans/my").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/diet-plans/user/*").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.POST, "/api/weekly-meal-plans").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.GET, "/api/weekly-meal-plans/dietician").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.GET, "/api/weekly-meal-plans/user/*").hasAnyRole("DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/weekly-meal-plans/my").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/weekly-meal-plans").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/notifications/my").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/notifications/*/read").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/notifications/read-all").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/health-tracking").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/health-tracking/my").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/health-tracking/*").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/health-tracking/user/*").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.GET, "/api/health-profile/user/*").hasAnyRole("DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reports/my-summary").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/reports/user/*").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.POST, "/api/reports/snapshots").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/meal-compliance/followed").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/meal-compliance/not-followed").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/meal-compliance/my").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/meal-compliance/user/*").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.GET, "/api/meal-compliance/dietician").hasRole("DIETICIAN")
                        .requestMatchers("/api/chat/**").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/subscriptions/create").hasAnyRole("USER", "DIETICIAN")
                        .requestMatchers(HttpMethod.POST, "/api/subscriptions/confirm").hasAnyRole("USER", "DIETICIAN")
                        .requestMatchers(HttpMethod.GET, "/api/subscriptions/my").hasAnyRole("USER", "DIETICIAN")
                        .requestMatchers(HttpMethod.GET, "/api/subscriptions/my-features").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/payments/create-order").hasAnyRole("USER", "DIETICIAN")
                        .requestMatchers(HttpMethod.POST, "/api/payments/confirm").hasAnyRole("USER", "DIETICIAN")
                        .requestMatchers(HttpMethod.POST, "/api/payments/verify").hasAnyRole("USER", "DIETICIAN")
                        .requestMatchers(HttpMethod.GET, "/api/payments/my").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/payments/dietician").hasRole("DIETICIAN")
                        .requestMatchers(HttpMethod.GET, "/api/payments/*/bill").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/subscriptions/*/bill").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/appointments/*/bill").hasAnyRole("USER", "DIETICIAN", "ADMIN")
                        .requestMatchers("/api/admin/twilio/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            String reason = authException == null || authException.getMessage() == null
                    ? "Authentication required"
                    : authException.getMessage();
            ApiErrorResponse body = ApiErrorResponse.builder()
                    .success(false)
                    .reason(reason)
                    .path(request.getRequestURI())
                    .build();
            response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
        };
    }

    @Bean
    public AccessDeniedHandler forbiddenAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            String reason = accessDeniedException == null || accessDeniedException.getMessage() == null
                    ? "Access denied"
                    : accessDeniedException.getMessage();
            ApiErrorResponse body = ApiErrorResponse.builder()
                    .success(false)
                    .reason(reason)
                    .path(request.getRequestURI())
                    .build();
            response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
        };
    }
}
