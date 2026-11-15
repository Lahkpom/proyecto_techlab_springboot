package com.proyecto.proyecto_techlab_springboot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiWebConfiguration implements WebMvcConfigurer {

    private final String apiPrefix;
    private final String[] allowedOrigins;

    public ApiWebConfiguration(
            @Value("${spring.mvc.api-prefix:/api}") String apiPrefix,
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String[] allowedOrigins
    ) {
        this.apiPrefix = apiPrefix;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(apiPrefix, HandlerTypePredicate.forAnnotation(RestController.class));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String corsPath = apiPrefix.endsWith("/")
                ? apiPrefix.substring(0, apiPrefix.length() - 1) + "/**"
                : apiPrefix + "/**";
        registry.addMapping(corsPath)
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
