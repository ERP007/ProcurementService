package org.fallguys.procurementservice.adapter.inbound.web;

import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtClaimExtractor {
    private JwtClaimExtractor() {}

    public static String extractUserCode(Jwt jwt) {
        String userCode = jwt.getClaimAsString("employee_no");
        if (userCode == null || userCode.isBlank()) {
            throw new ForbiddenException(ProcurementErrorCode.UNAUTHORIZED);
        }
        return userCode;
    }

    public static UserRole extractRole(Jwt jwt) {
        try {
            String role = jwt.getClaimAsString("user_role");
            return UserRole.valueOf(role);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ForbiddenException(ProcurementErrorCode.UNAUTHORIZED);
        }
    }
}
