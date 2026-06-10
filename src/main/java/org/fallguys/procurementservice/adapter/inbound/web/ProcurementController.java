package org.fallguys.procurementservice.adapter.inbound.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.adapter.inbound.web.dto.ApprovePurchaseOrderResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.CancelPurchaseOrderRequest;
import org.fallguys.procurementservice.adapter.inbound.web.dto.CancelPurchaseOrderResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.DraftPurchaseOrderRequest;
import org.fallguys.procurementservice.adapter.inbound.web.dto.CreatePurchaseOrderRequest;
import org.fallguys.procurementservice.adapter.inbound.web.dto.CreatePurchaseOrderResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.PurchaseOrderDetailResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.PurchaseOrderHistoryResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.ReceivePurchaseOrderRequest;
import org.fallguys.procurementservice.adapter.inbound.web.dto.ReceivePurchaseOrderResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.PurchaseOrderKpiResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.PurchaseOrderPageResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.SearchPurchaseOrderRequest;
import org.fallguys.procurementservice.adapter.inbound.web.dto.VendorResponse;
import org.fallguys.procurementservice.application.port.inbound.ApprovePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.ApprovePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.CancelPurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.CancelPurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.CreatePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.GetPurchaseOrderKpiUseCase;
import org.fallguys.procurementservice.application.port.inbound.GetPurchaseOrderHistoriesUseCase;
import org.fallguys.procurementservice.application.port.inbound.GetPurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.ReceivePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.ReceivePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.SearchActiveVendorsUseCase;
import org.fallguys.procurementservice.application.port.inbound.SearchPurchaseOrderUseCase;
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
    private final ApprovePurchaseOrderUseCase approvePurchaseOrderUseCase;
    private final GetPurchaseOrderKpiUseCase getPurchaseOrderKpiUseCase;
    private final SearchPurchaseOrderUseCase searchPurchaseOrderUseCase;
    private final GetPurchaseOrderUseCase getPurchaseOrderUseCase;
    private final GetPurchaseOrderHistoriesUseCase getPurchaseOrderHistoriesUseCase;
    private final ReceivePurchaseOrderUseCase receivePurchaseOrderUseCase;
    private final CancelPurchaseOrderUseCase cancelPurchaseOrderUseCase;

    @GetMapping("/{code}")
    public ResponseEntity<PurchaseOrderDetailResponse> getDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        return ResponseEntity.ok(PurchaseOrderDetailResponse.from(getPurchaseOrderUseCase.get(role, code)));
    }

    @GetMapping("/{code}/histories")
    public ResponseEntity<List<PurchaseOrderHistoryResponse>> getHistories(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        return ResponseEntity.ok(PurchaseOrderHistoryResponse.listFrom(
                getPurchaseOrderHistoriesUseCase.getHistories(role, code)
        ));
    }

    @GetMapping("/kpi")
    public ResponseEntity<PurchaseOrderKpiResponse> getKpi(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        return ResponseEntity.ok(PurchaseOrderKpiResponse.from(getPurchaseOrderKpiUseCase.getKpi(role)));
    }

    @GetMapping
    public ResponseEntity<PurchaseOrderPageResponse> search(
            @AuthenticationPrincipal Jwt jwt,
            @ModelAttribute @Valid SearchPurchaseOrderRequest request
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        return ResponseEntity.ok(PurchaseOrderPageResponse.from(
                searchPurchaseOrderUseCase.search(role, request.toQuery())
        ));
    }

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

    @PatchMapping("/{code}/receive")
    public ResponseEntity<ReceivePurchaseOrderResponse> receive(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code,
            @RequestBody @Valid ReceivePurchaseOrderRequest request
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        PurchaseOrder received = receivePurchaseOrderUseCase.receive(role,
                new ReceivePurchaseOrderCommand(code, userCode, request.receivedDate()));
        return ResponseEntity.ok(ReceivePurchaseOrderResponse.from(received));
    }

    @PatchMapping("/{code}/approve")
    public ResponseEntity<ApprovePurchaseOrderResponse> approve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        PurchaseOrder approved = approvePurchaseOrderUseCase.approve(role, new ApprovePurchaseOrderCommand(code, userCode));
        return ResponseEntity.ok(ApprovePurchaseOrderResponse.from(approved));
    }

    @PatchMapping("/{code}/cancel")
    public ResponseEntity<CancelPurchaseOrderResponse> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code,
            @RequestBody @Valid CancelPurchaseOrderRequest request
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        PurchaseOrder canceled = cancelPurchaseOrderUseCase.cancel(role, new CancelPurchaseOrderCommand(code, userCode, request.reason()));
        return ResponseEntity.ok(CancelPurchaseOrderResponse.from(canceled));
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
