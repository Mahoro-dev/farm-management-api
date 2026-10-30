package rw.farm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI farmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Farm Management API")
                        .version("1.0.0")
                        .description("""
                                REST API for one organization with multiple cattle farms.
                                Every monetary amount is Rwandan francs (RWF). The API has no currency field and does not convert money.
                                Call POST /api/auth/bootstrap once, then POST /api/auth/login and send the token as Authorization: Bearer.
                                """))
                .addSecurityItem(new SecurityRequirement().addList("bearer"))
                .components(new Components().addSecuritySchemes("bearer", new SecurityScheme()
                        .name("bearer")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
