package com.proyecto.proyecto_techlab_springboot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    private static final String API_PREFIX = "/api";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            new PublicRoute(RequestMethod.POST, "/auth/login"),
            new PublicRoute(RequestMethod.POST, "/auth/register"),
            new PublicRoute(RequestMethod.POST, "/auth/change-expired-password"),
            new PublicRoute(RequestMethod.POST, "/auth/refresh"),
            new PublicRoute(RequestMethod.POST, "/auth/logout"),
            new PublicRoute(RequestMethod.GET, "/products/**"),
            new PublicRoute(RequestMethod.GET, "/variants/**"),
            new PublicRoute(RequestMethod.GET, "/colors/**"),
            new PublicRoute(RequestMethod.GET, "/shipping/**"),
            new PublicRoute(RequestMethod.POST, "/cart/sync"),
            new PublicRoute(RequestMethod.POST, "/sales/checkout"),
            new PublicRoute(RequestMethod.GET, "/sales/track/**"),
            new PublicRoute(RequestMethod.POST, "/returns/guest/**"),
            new PublicRoute(RequestMethod.GET, "/returns/track/**"),
            new PublicRoute(RequestMethod.POST, "/payments/mercadopago/webhook"),
            new PublicRoute(RequestMethod.POST, "/dev/**")
    );

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-commerce API")
                        .description("""
                                API REST de e-commerce para catálogo, usuarios, ventas y operaciones administrativas.

                                Roles de prueba: ADMIN, OPERATOR, WHOLESALER y CUSTOMER. Use cuentas de prueba
                                configuradas para el entorno; no se incluyen credenciales en esta documentación.
                                """)
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OperationCustomizer publicOperationsCustomizer(
            RequestMappingHandlerMapping requestMappingHandlerMapping
    ) {
        Map<HandlerMethod, List<RequestMappingInfo>> mappingsByHandler = new HashMap<>();
        requestMappingHandlerMapping.getHandlerMethods().forEach((mapping, handlerMethod) ->
                mappingsByHandler.computeIfAbsent(handlerMethod, ignored -> new ArrayList<>()).add(mapping)
        );

        return (operation, handlerMethod) -> {
            boolean isPublic = mappingsByHandler.getOrDefault(handlerMethod, List.of())
                    .stream()
                    .anyMatch(mapping -> isPublicRoute(mapping, handlerMethod));
            if (isPublic) {
                operation.setSecurity(List.of());
            }
            return operation;
        };
    }

    private static boolean isPublicRoute(RequestMappingInfo mapping, HandlerMethod handlerMethod) {
        if (hasNonAnonymousPreAuthorize(handlerMethod)) {
            return false;
        }

        Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
        return mapping.getPatternValues().stream()
                .map(OpenApiConfig::removeApiPrefix)
                .anyMatch(path -> PUBLIC_ROUTES.stream().anyMatch(route ->
                        (methods.isEmpty() || methods.contains(route.method()))
                                && PATH_MATCHER.match(route.path(), path)
                ));
    }

    private static boolean hasNonAnonymousPreAuthorize(HandlerMethod handlerMethod) {
        PreAuthorize methodPreAuthorize = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(),
                PreAuthorize.class
        );
        PreAuthorize typePreAuthorize = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getBeanType(),
                PreAuthorize.class
        );
        return isNonAnonymous(methodPreAuthorize) || isNonAnonymous(typePreAuthorize);
    }

    private static boolean isNonAnonymous(PreAuthorize preAuthorize) {
        return preAuthorize != null && !preAuthorize.value().trim().equals("isAnonymous()");
    }

    private static String removeApiPrefix(String path) {
        if (path.equals(API_PREFIX)) {
            return "/";
        }
        return path.startsWith(API_PREFIX + "/") ? path.substring(API_PREFIX.length()) : path;
    }

    private record PublicRoute(RequestMethod method, String path) {
    }
}
