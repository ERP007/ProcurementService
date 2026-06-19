package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.inbound.command.PurchaseOrderLineCommand;
import org.fallguys.procurementservice.application.port.inbound.command.UpdatePurchaseOrderDraftCommand;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadVendorPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadWarehousePort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.*;
import org.fallguys.procurementservice.domain.model.purchaseorder.ProcurementOrderCreation;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.vendor.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UpdatePurchaseOrderDraftServiceTest {

    @Mock private LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock private LoadVendorPort loadVendorPort;
    @Mock private LoadWarehousePort loadWarehousePort;
    @Mock private SavePurchaseOrderPort savePurchaseOrderPort;

    @InjectMocks
    private UpdatePurchaseOrderDraftService service;

    private Vendor vendor;
    private PurchaseOrder draftPo;
    private ProcurementOrderCreation originalCreation;

    @BeforeEach
    void setUp() {
        vendor = new Vendor("VD-001", "㈜동성정밀", "김담당", "010-1234-5678", "서울시", true);
        originalCreation = new ProcurementOrderCreation("EMP-001", Instant.parse("2026-06-01T00:00:00Z"));
        draftPo = new PurchaseOrder(
                "PO-2026-06-0001", "VD-001", "HQ-SE-01",
                PurchaseOrderStatus.DRAFT,
                LocalDate.now().plusDays(7), "기존 메모",
                List.of(),
                Money.of(BigDecimal.ZERO),
                originalCreation, null, null, null
        );
    }

    // ── 역할 검증 ──────────────────────────────────────────────────────────

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.update(UserRole.BRANCH_MANAGER, validCommand()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadVendorPort, loadWarehousePort, savePurchaseOrderPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.update(UserRole.BRANCH_STAFF, validCommand()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadVendorPort, loadWarehousePort, savePurchaseOrderPort);
    }

    // ── 발주서 조회 ────────────────────────────────────────────────────────

    @Test
    void 발주서_미존재이면_ResourceNotFoundException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(UserRole.ADMIN, validCommand()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadVendorPort, loadWarehousePort, savePurchaseOrderPort);
    }

    // ── 상태 검증 ──────────────────────────────────────────────────────────

    @Test
    void DRAFT_아닌_상태이면_BusinessValidationException_발생() {
        PurchaseOrder approvedPo = new PurchaseOrder(
                "PO-2026-06-0001", "VD-001", "HQ-SE-01",
                PurchaseOrderStatus.APPROVED,
                LocalDate.now().plusDays(7), null,
                List.of(),
                Money.of(BigDecimal.ZERO),
                originalCreation, null, null, null
        );
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(approvedPo));

        assertThatThrownBy(() -> service.update(UserRole.ADMIN, validCommand()))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(loadVendorPort, loadWarehousePort, savePurchaseOrderPort);
    }

    // ── 도착 희망일 검증 ───────────────────────────────────────────────────

    @Test
    void 도착희망일이_1년_초과이면_BusinessValidationException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPo));

        assertThatThrownBy(() -> service.update(UserRole.ADMIN, commandWithDate(LocalDate.now().plusYears(1).plusDays(1))))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(loadVendorPort, loadWarehousePort, savePurchaseOrderPort);
    }

    @Test
    void 도착희망일이_정확히_1년이면_통과() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPo));
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-01");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        assertThat(service.update(UserRole.ADMIN, commandWithDate(LocalDate.now().plusYears(1)))).isNotNull();
    }

    // ── 중복 품목 검증 ─────────────────────────────────────────────────────

    @Test
    void 동일_itemSku_중복이면_BusinessValidationException_발생() {
        PurchaseOrderLineCommand line = new PurchaseOrderLineCommand("SKU-001", 10, BigDecimal.valueOf(8400));
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPo));

        assertThatThrownBy(() -> service.update(UserRole.ADMIN, commandWithLines(List.of(line, line))))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(loadVendorPort, loadWarehousePort, savePurchaseOrderPort);
    }

    // ── 공급사·창고 조회 ───────────────────────────────────────────────────

    @Test
    void 공급사_미존재이면_ResourceNotFoundException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPo));
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(UserRole.ADMIN, validCommand()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadWarehousePort, savePurchaseOrderPort);
    }

    @Test
    void 창고_비활성이면_ResourceNotFoundException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPo));
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willThrow(new ResourceNotFoundException(ProcurementErrorCode.WAREHOUSE_NOT_FOUND))
                .given(loadWarehousePort).verifyActive("HQ-SE-01");

        assertThatThrownBy(() -> service.update(UserRole.ADMIN, validCommand()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(savePurchaseOrderPort);
    }

    // ── 성공 ───────────────────────────────────────────────────────────────

    @Test
    void 수정_성공_코드와_creation_유지() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPo));
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-01");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.update(UserRole.ADMIN, validCommand());

        assertThat(result.getCode()).isEqualTo("PO-2026-06-0001");
        assertThat(result.getCreation().createdBy()).isEqualTo("EMP-001");
        assertThat(result.getCreation().createdAt()).isEqualTo(originalCreation.createdAt());
    }

    @Test
    void 수정_성공_필드_반영() {
        PurchaseOrderLineCommand lineCmd = new PurchaseOrderLineCommand("SKU-001", 5, BigDecimal.valueOf(10000));
        UpdatePurchaseOrderDraftCommand command = new UpdatePurchaseOrderDraftCommand(
                "PO-2026-06-0001", "VD-002", "HQ-SE-02",
                LocalDate.now().plusDays(14), "수정된 메모",
                List.of(lineCmd)
        );
        Vendor vendor2 = new Vendor("VD-002", "㈜한국부품", "이담당", "010-9876-5432", "부산시", true);

        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPo));
        given(loadVendorPort.findActiveByCode("VD-002")).willReturn(Optional.of(vendor2));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-02");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.update(UserRole.ADMIN, command);

        assertThat(result.getVendorCode()).isEqualTo("VD-002");
        assertThat(result.getWarehouseCode()).isEqualTo("HQ-SE-02");
        assertThat(result.getMemo()).isEqualTo("수정된 메모");
        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(result.getLines()).hasSize(1);
        assertThat(result.getLines().get(0).lineAmount().amount())
                .isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    void 수정_성공_approval은_null() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-06-0001")).willReturn(Optional.of(draftPo));
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-01");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.update(UserRole.ADMIN, validCommand());

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(savePurchaseOrderPort).save(captor.capture());

        PurchaseOrder saved = captor.getValue();
        assertThat(saved.getApproval()).isNull();
        assertThat(saved.getReceiving()).isNull();
        assertThat(saved.getCancellation()).isNull();
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────

    private UpdatePurchaseOrderDraftCommand validCommand() {
        return commandWithLines(List.of());
    }

    private UpdatePurchaseOrderDraftCommand commandWithLines(List<PurchaseOrderLineCommand> lines) {
        return new UpdatePurchaseOrderDraftCommand(
                "PO-2026-06-0001", "VD-001", "HQ-SE-01",
                LocalDate.now().plusDays(7), "수정 메모", lines
        );
    }

    private UpdatePurchaseOrderDraftCommand commandWithDate(LocalDate date) {
        return new UpdatePurchaseOrderDraftCommand(
                "PO-2026-06-0001", "VD-001", "HQ-SE-01",
                date, null, List.of()
        );
    }
}
