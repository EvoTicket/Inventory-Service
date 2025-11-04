package com.capstone.inventoryservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtUtil {
    public TokenMetaData getDataFromAuth(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        Long userId = jwt.getClaim("userId");
        Boolean isOrganization = jwt.getClaim("isOrganization");
        Long organizationId = jwt.getClaim("organizationId");

        return new TokenMetaData(userId, isOrganization, organizationId);
    }
}
