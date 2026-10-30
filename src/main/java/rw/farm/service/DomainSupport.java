package rw.farm.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import rw.farm.domain.User;
import rw.farm.security.SecurityUtils;

import java.util.UUID;

@Component
public class DomainSupport {

    @PersistenceContext
    private EntityManager entityManager;

    public User actor() {
        return entityManager.getReference(User.class, SecurityUtils.userId());
    }

    public void flush() {
        entityManager.flush();
    }

    public void refresh(Object entity) {
        entityManager.refresh(entity);
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    public static UUID placeholderFarm() {
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }
}
