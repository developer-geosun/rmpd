package com.geosun.rmpd.infrastructure.security;

import com.geosun.rmpd.domain.model.User;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser requireCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("Користувач не автентифікований");
        }
        return user;
    }

    public static Long requireCarrierId() {
        return requireCurrentUser().carrierId();
    }

    public static AuthenticatedUser fromEntity(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getCarrier().getId(),
                user.getEmail(),
                user.getRole(),
                user.getPasswordHash());
    }
}
