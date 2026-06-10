package org.fallguys.procurementservice.adapter.inbound.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.adapter.inbound.web.dto.DraftPurchaseOrderRequest;
import org.fallguys.procurementservice.adapter.inbound.web.dto.CreatePurchaseOrderRequest;
import org.fallguys.procurementservice.adapter.inbound.web.dto.CreatePurchaseOrderResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.VendorResponse;
import org.fallguys.procurementservice.application.port.inbound.CreatePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.SearchActiveVendorsUseCase;
import org.fallguys.procurementservice.application.port.inbound.UpdatePurchaseOrderDraftUseCase;
import org.fallguys.procurementservice.domain.model.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/procurement-orders")
@RequiredArgsConstructor
public class ProcurementController {

    private final SearchActiveVendorsUseCase searchActiveVendorsUseCase;
    private final CreatePurchaseOrderUseCase createPurchaseOrderUseCase;
    private final UpdatePurchaseOrderDraftUseCase updatePurchaseOrderDraftUseCase;

    @GetMapping("/vendors")
    public ResponseEntity<List<VendorResponse>> searchActiveVendors(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String search
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        List<VendorResponse> result = searchActiveVendorsUseCase.searchActiveVendors(role, search)
                .stream()
                .map(VendorResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/drafts")
    public ResponseEntity<CreatePurchaseOrderResponse> createDraft(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid DraftPurchaseOrderRequest request
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        PurchaseOrder created = createPurchaseOrderUseCase.create(role, request.toCommand(userCode));
        return ResponseEntity.status(HttpStatus.CREATED).body(CreatePurchaseOrderResponse.from(created));
    }

    @PutMapping("/{code}")
    public ResponseEntity<CreatePurchaseOrderResponse> updateDraft(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code,
            @RequestBody @Valid DraftPurchaseOrderRequest request
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        PurchaseOrder updated = updatePurchaseOrderDraftUseCase.update(role, request.toUpdateCommand(code));
        return ResponseEntity.ok(CreatePurchaseOrderResponse.from(updated));
    }

    @PostMapping
    public ResponseEntity<CreatePurchaseOrderResponse> createPurchaseOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreatePurchaseOrderRequest request
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        PurchaseOrder created = createPurchaseOrderUseCase.create(role, request.toCommand(userCode));
        return ResponseEntity.status(HttpStatus.CREATED).body(CreatePurchaseOrderResponse.from(created));
    }
}
