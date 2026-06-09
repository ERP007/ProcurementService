package org.fallguys.procurementservice.adapter.inbound.web;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.adapter.inbound.web.dto.VendorResponse;
import org.fallguys.procurementservice.application.port.inbound.SearchActiveVendorsUseCase;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/procurement-orders")
@RequiredArgsConstructor
public class VendorController {

    private final SearchActiveVendorsUseCase searchActiveVendorsUseCase;

    @GetMapping("/vendors")
    public List<VendorResponse> searchActiveVendors(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String search
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        return searchActiveVendorsUseCase.searchActiveVendors(role, search)
                .stream()
                .map(VendorResponse::from)
                .toList();
    }
}
