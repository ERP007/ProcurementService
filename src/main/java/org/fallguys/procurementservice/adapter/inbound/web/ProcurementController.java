package org.fallguys.procurementservice.adapter.inbound.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.adapter.inbound.web.dto.CancelPurchaseOrderRequest;
import org.fallguys.procurementservice.adapter.inbound.web.dto.DraftPurchaseOrderRequest;
import org.fallguys.procurementservice.adapter.inbound.web.dto.CreatePurchaseOrderRequest;
import org.fallguys.procurementservice.adapter.inbound.web.dto.PurchaseOrderDetailResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.PurchaseOrderProgressResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.PurchaseOrderHistoryResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.PurchaseOrderStatusResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.ReceivePurchaseOrderRequest;
import org.fallguys.procurementservice.adapter.inbound.web.dto.PurchaseOrderKpiResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.PurchaseOrderPageResponse;
import org.fallguys.procurementservice.adapter.inbound.web.dto.SearchPurchaseOrderRequest;
import org.fallguys.procurementservice.adapter.inbound.web.dto.VendorResponse;
import org.fallguys.procurementservice.application.port.inbound.command.ApprovePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.usecase.ApprovePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.command.CancelPurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.usecase.CancelPurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.usecase.CreatePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.usecase.GetPurchaseOrderKpiUseCase;
import org.fallguys.procurementservice.application.port.inbound.usecase.GetPurchaseOrderHistoriesUseCase;
import org.fallguys.procurementservice.application.port.inbound.usecase.GetPurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.usecase.GetPurchaseOrderProgressUseCase;
import org.fallguys.procurementservice.application.port.inbound.command.ReceivePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.usecase.ReceivePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.usecase.SearchActiveVendorsUseCase;
import org.fallguys.procurementservice.application.port.inbound.usecase.SearchPurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.inbound.usecase.UpdatePurchaseOrderDraftUseCase;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ProcurementOrder", description = "구매 발주 API")
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
    private final GetPurchaseOrderProgressUseCase getPurchaseOrderProgressUseCase;
    private final GetPurchaseOrderHistoriesUseCase getPurchaseOrderHistoriesUseCase;
    private final ReceivePurchaseOrderUseCase receivePurchaseOrderUseCase;
    private final CancelPurchaseOrderUseCase cancelPurchaseOrderUseCase;

    @Operation(summary = "구매 발주 상세 조회")
    @GetMapping("/{code}")
    public ResponseEntity<PurchaseOrderDetailResponse> getDetail(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        return ResponseEntity.ok(PurchaseOrderDetailResponse.from(getPurchaseOrderUseCase.get(role, code)));
    }

    @Operation(summary = "구매 발주 진행 상태 조회", description = "status·saga 조합으로 파생한 진행 상태를 폴링용으로 반환한다.")
    @GetMapping("/{code}/progress")
    public ResponseEntity<PurchaseOrderProgressResponse> getProgress(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        return ResponseEntity.ok(PurchaseOrderProgressResponse.from(getPurchaseOrderProgressUseCase.get(role, code)));
    }

    @Operation(summary = "구매 발주 이력 조회", description = "발주 상태 변경 이력을 최신순으로 반환한다.")
    @GetMapping("/{code}/histories")
    public ResponseEntity<List<PurchaseOrderHistoryResponse>> getHistories(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        return ResponseEntity.ok(PurchaseOrderHistoryResponse.listFrom(
                getPurchaseOrderHistoriesUseCase.getHistories(role, code)
        ));
    }

    @Operation(summary = "구매 발주 KPI 조회")
    @GetMapping("/kpi")
    public ResponseEntity<PurchaseOrderKpiResponse> getKpi(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        return ResponseEntity.ok(PurchaseOrderKpiResponse.from(getPurchaseOrderKpiUseCase.getKpi(role)));
    }

    @Operation(summary = "구매 발주 목록 조회", description = "날짜 범위·상태·벤더 필터와 페이지네이션으로 발주 목록을 조회한다.")
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

    @Operation(summary = "활성 벤더 목록 조회", description = "active=true 벤더를 검색어로 필터링해 반환한다.")
    @GetMapping("/vendors")
    public ResponseEntity<List<VendorResponse>> searchActiveVendors(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "벤더명·코드 검색어 (선택)") @RequestParam(required = false) String search
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        List<VendorResponse> result = searchActiveVendorsUseCase.searchActiveVendors(role, search)
                .stream()
                .map(VendorResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "구매 발주 임시저장", description = "DRAFT 상태로 발주를 생성한다.")
    @PostMapping("/drafts")
    public ResponseEntity<PurchaseOrderStatusResponse> createDraft(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid DraftPurchaseOrderRequest request
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        String userName = JwtClaimExtractor.extractUserName(jwt);
        String userPosition = JwtClaimExtractor.extractPosition(jwt);
        PurchaseOrder created = createPurchaseOrderUseCase.create(role, request.toCommand(userCode, userName, userPosition));
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseOrderStatusResponse.from(created));
    }

    @Operation(summary = "구매 발주 임시저장 수정", description = "DRAFT 발주 내용을 수정한다.")
    @PutMapping("/{code}")
    public ResponseEntity<PurchaseOrderStatusResponse> updateDraft(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code,
            @RequestBody @Valid DraftPurchaseOrderRequest request
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        PurchaseOrder updated = updatePurchaseOrderDraftUseCase.update(role, request.toUpdateCommand(code, userCode));
        return ResponseEntity.ok(PurchaseOrderStatusResponse.from(updated));
    }

    @Operation(summary = "구매 발주 입고 처리", description = "APPROVED 발주를 RECEIVED로 전환하고 재고 입고를 기록한다.")
    @PatchMapping("/{code}/receive")
    public ResponseEntity<PurchaseOrderStatusResponse> receive(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code,
            @RequestBody @Valid ReceivePurchaseOrderRequest request
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        String userName = JwtClaimExtractor.extractUserName(jwt);
        String userPosition = JwtClaimExtractor.extractPosition(jwt);
        PurchaseOrder received = receivePurchaseOrderUseCase.receive(role,
                new ReceivePurchaseOrderCommand(code, userCode, userName, userPosition, request.receivedDate()));
        return ResponseEntity.ok(PurchaseOrderStatusResponse.from(received));
    }

    @Operation(summary = "구매 발주 승인", description = "REQUESTED 발주를 APPROVED로 전환한다.")
    @PatchMapping("/{code}/approve")
    public ResponseEntity<PurchaseOrderStatusResponse> approve(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        String userName = JwtClaimExtractor.extractUserName(jwt);
        String userPosition = JwtClaimExtractor.extractPosition(jwt);
        PurchaseOrder approved = approvePurchaseOrderUseCase.approve(role,
                new ApprovePurchaseOrderCommand(code, userCode, userName, userPosition));
        return ResponseEntity.ok(PurchaseOrderStatusResponse.from(approved));
    }

    @Operation(summary = "구매 발주 취소", description = "발주를 CANCELED로 전환한다.")
    @PatchMapping("/{code}/cancel")
    public ResponseEntity<PurchaseOrderStatusResponse> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code,
            @RequestBody @Valid CancelPurchaseOrderRequest request
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        String userName = JwtClaimExtractor.extractUserName(jwt);
        String userPosition = JwtClaimExtractor.extractPosition(jwt);
        PurchaseOrder canceled = cancelPurchaseOrderUseCase.cancel(role,
                new CancelPurchaseOrderCommand(code, userCode, userName, userPosition, request.reason()));
        return ResponseEntity.ok(PurchaseOrderStatusResponse.from(canceled));
    }

    @Operation(summary = "구매 발주 생성(즉시 제출)", description = "임시저장 없이 APPROVED 상태로 발주를 즉시 생성한다.")
    @PostMapping
    public ResponseEntity<PurchaseOrderStatusResponse> createPurchaseOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreatePurchaseOrderRequest request
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        String userName = JwtClaimExtractor.extractUserName(jwt);
        String userPosition = JwtClaimExtractor.extractPosition(jwt);
        PurchaseOrder created = createPurchaseOrderUseCase.create(role, request.toCommand(userCode, userName, userPosition));
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseOrderStatusResponse.from(created));
    }
}
