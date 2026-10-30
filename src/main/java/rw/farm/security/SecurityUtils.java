package rw.farm.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import rw.farm.common.ApiException;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UserPrincipal requirePrincipal() {
        return principal().orElseThrow(() -> ApiException.unauthorized("Authentication required"));
    }

    public static UUID userId() {
        return requirePrincipal().id();
    }

    public static UUID organizationId() {
        return requirePrincipal().organizationId();
    }

    public static String userIdOrNull() {
        return principal().map(principal -> principal.id().toString()).orElse(null);
    }
}
