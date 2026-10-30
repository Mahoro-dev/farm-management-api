package rw.farm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Breeding breeding) {

    public record Jwt(String secret, int ttlHours) {
    }

    public record Breeding(int maxGenerations) {
    }
}
