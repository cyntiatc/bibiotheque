package com.ibizabroker.bibliotheque.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Déclare les métadonnées de l'API (titre/version/description affichés en
 * haut de Swagger UI, à la place du "OpenAPI definition" générique) ainsi
 * que le schéma "Bearer JWT" pour que le bouton "Authorize" (cadenas en
 * haut à droite) apparaisse. Sans ce dernier, springdoc ne sait pas que
 * l'API attend un header "Authorization: Bearer <token>" et le bouton
 * n'apparaît pas du tout.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI bibliothequeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gestion de Bibliothèque API")
                        .version("1.0.0")
                        .description("API REST pour la gestion de la bibliothèque et le module de réservation"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
