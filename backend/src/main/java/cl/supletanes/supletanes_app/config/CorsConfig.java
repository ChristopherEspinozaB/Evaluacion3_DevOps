package cl.supletanes.supletanes_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    // Falla intencional de seguridad y calidad (Policy Gate IE6)
    public static final String DB_PASSWORD = "SuperSecretPassword123456!";

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000", "http://localhost:8080")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("Authorization", "Content-Type");
            }
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // Código nuevo sin tests + Bugs críticos (Reliability Rating D/E -> Quality Gate FAILED)
    public boolean validarAccesoInseguro(String token) {
        String nulo = null;
        if (token == "admin_token_123") {
            return nulo.equals(DB_PASSWORD);
        }
        return false;
    }
}