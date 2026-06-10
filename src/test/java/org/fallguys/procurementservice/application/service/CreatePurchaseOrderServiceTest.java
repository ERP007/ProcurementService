package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.inbound.CreatePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.PurchaseOrderLineCommand;
import org.fallguys.procurementservice.application.port.outbound.*;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
class CreatePurchaseOrderServiceTest {

    @Mock private LoadVendorPort loadVendorPort;
    @Mock private LoadWarehousePort loadWarehousePort;
    @Mock private LoadItemPort loadItemPort;
    @Mock private GeneratePoCodePort generatePoCodePort;
    @Mock private SavePurchaseOrderPort savePurchaseOrderPort;

    @InjectMocks
    private CreatePurchaseOrderService service;

    private Vendor vendor;

    @BeforeEach
    void setUp() {
        vendor = new Vendor("VD-001", "㈜동성정밀", "김담당", "010-1234-5678", "서울시", true);
    }

    // ── 역할 검증 ──────────────────────────────────────────────────────────

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.create(UserRole.BRANCH_MANAGER, draftCommandWithoutLines()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadVendorPort, loadWarehousePort, loadItemPort, generatePoCodePort, savePurchaseOrderPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.create(UserRole.BRANCH_STAFF, draftCommandWithoutLines()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadVendorPort, loadWarehousePort, loadItemPort, generatePoCodePort, savePurchaseOrderPort);
    }

    // ── 도착 희망일 검증 ───────────────────────────────────────────────────

    @Test
    void 도착희망일이_1년_초과이면_BusinessValidationException_발생() {
        assertThatThrownBy(() -> service.create(UserRole.ADMIN, commandWithDate(LocalDate.now().plusYears(1).plusDays(1))))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(loadVendorPort, loadWarehousePort, loadItemPort, generatePoCodePort, savePurchaseOrderPort);
    }

    @Test
    void 도착희망일이_정확히_1년이면_통과() {
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-01");
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(UserRole.ADMIN, commandWithDate(LocalDate.now().plusYears(1)))).isNotNull();
    }

    // ── 중복 품목 검증 ─────────────────────────────────────────────────────

    @Test
    void 동일_itemSku_중복이면_BusinessValidationException_발생() {
        PurchaseOrderLineCommand line = new PurchaseOrderLineCommand("SKU-001", 10, BigDecimal.valueOf(8400));

        assertThatThrownBy(() -> service.create(UserRole.ADMIN, draftCommandWithLines(List.of(line, line))))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(loadVendorPort, loadWarehousePort, loadItemPort, generatePoCodePort, savePurchaseOrderPort);
    }

    // ── 공급사·창고 조회 ───────────────────────────────────────────────────

    @Test
    void 공급사_미존재이면_ResourceNotFoundException_발생() {
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(UserRole.ADMIN, draftCommandWithoutLines()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadWarehousePort, loadItemPort, generatePoCodePort, savePurchaseOrderPort);
    }

    @Test
    void 창고_미존재이면_ResourceNotFoundException_발생() {
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willThrow(new ResourceNotFoundException(ProcurementErrorCode.WAREHOUSE_NOT_FOUND))
                .given(loadWarehousePort).verifyActive("HQ-SE-01");

        assertThatThrownBy(() -> service.create(UserRole.ADMIN, draftCommandWithoutLines()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadItemPort, generatePoCodePort, savePurchaseOrderPort);
    }

    // ── DRAFT 성공 ─────────────────────────────────────────────────────────

    @Test
    void 라인_없이_초안_생성_성공() {
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-01");
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.create(UserRole.HQ_MANAGER, draftCommandWithoutLines());

        assertThat(result.getCode()).isEqualTo("PO-2026-06-0001");
        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(result.getLines()).isEmpty();
    }

    @Test
    void 라인_포함_초안_생성_성공_스냅샷은_null() {
        PurchaseOrderLineCommand lineCmd = new PurchaseOrderLineCommand("SKU-001", 10, BigDecimal.valueOf(8400));

        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-01");
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.create(UserRole.HQ_STAFF, draftCommandWithLines(List.of(lineCmd)));

        PurchaseOrderLine line = result.getLines().get(0);
        assertThat(line.getItemSku()).isEqualTo("SKU-001");
        assertThat(line.getItemNameSnapshot()).isNull();
        assertThat(line.getUnitSnapshot()).isNull();
        assertThat(line.getOrderQuantity()).isEqualTo(10);
        assertThat(line.getLineAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(84000));
    }

    @Test
    void 초안_저장_시_approval은_null() {
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-01");
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.create(UserRole.ADMIN, draftCommandWithoutLines());

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(savePurchaseOrderPort).save(captor.capture());

        PurchaseOrder saved = captor.getValue();
        assertThat(saved.getCreation().createdBy()).isEqualTo("EMP-001");
        assertThat(saved.getApproval()).isNull();
        assertThat(saved.getReceiving()).isNull();
        assertThat(saved.getCancellation()).isNull();
    }

    // ── APPROVED 성공 ──────────────────────────────────────────────────────

    @Test
    void 존재하지_않는_SKU이면_ResourceNotFoundException_발생() {
        PurchaseOrderLineCommand lineCmd = new PurchaseOrderLineCommand("SKU-999", 5, BigDecimal.valueOf(1000));

        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-01");
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(loadItemPort.loadAll(List.of("SKU-999"))).willReturn(Map.of());

        assertThatThrownBy(() -> service.create(UserRole.ADMIN, approvedCommandWithLines(List.of(lineCmd))))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(savePurchaseOrderPort);
    }

    @Test
    void APPROVED_생성_성공_스냅샷_채워짐() {
        PurchaseOrderLineCommand lineCmd = new PurchaseOrderLineCommand("SKU-001", 5, BigDecimal.valueOf(10000));
        ItemInfo itemInfo = new ItemInfo("SKU-001", "브레이크 패드", "EA");

        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-01");
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(loadItemPort.loadAll(List.of("SKU-001"))).willReturn(Map.of("SKU-001", itemInfo));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.create(UserRole.ADMIN, approvedCommandWithLines(List.of(lineCmd)));

        PurchaseOrderLine line = result.getLines().get(0);
        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(line.getItemNameSnapshot()).isEqualTo("브레이크 패드");
        assertThat(line.getUnitSnapshot()).isEqualTo("EA");
        assertThat(line.getLineAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    void APPROVED_생성_성공_approval_세팅됨() {
        PurchaseOrderLineCommand lineCmd = new PurchaseOrderLineCommand("SKU-001", 1, BigDecimal.valueOf(1000));
        ItemInfo itemInfo = new ItemInfo("SKU-001", "브레이크 패드", "EA");

        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-01");
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(loadItemPort.loadAll(List.of("SKU-001"))).willReturn(Map.of("SKU-001", itemInfo));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.create(UserRole.ADMIN, approvedCommandWithLines(List.of(lineCmd)));

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(savePurchaseOrderPort).save(captor.capture());

        PurchaseOrder saved = captor.getValue();
        assertThat(saved.getApproval()).isNotNull();
        assertThat(saved.getApproval().approvedBy()).isEqualTo("EMP-001");
        assertThat(saved.getApproval().approvedAt()).isNotNull();
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────

    private CreatePurchaseOrderCommand draftCommandWithoutLines() {
        return draftCommandWithLines(List.of());
    }

    private CreatePurchaseOrderCommand draftCommandWithLines(List<PurchaseOrderLineCommand> lines) {
        return new CreatePurchaseOrderCommand(
                "EMP-001", "VD-001", "HQ-SE-01",
                LocalDate.now().plusDays(7), "정기 발주 건", lines,
                PurchaseOrderStatus.DRAFT
        );
    }

    private CreatePurchaseOrderCommand approvedCommandWithLines(List<PurchaseOrderLineCommand> lines) {
        return new CreatePurchaseOrderCommand(
                "EMP-001", "VD-001", "HQ-SE-01",
                LocalDate.now().plusDays(7), "정기 발주 건", lines,
                PurchaseOrderStatus.APPROVED
        );
    }

    private CreatePurchaseOrderCommand commandWithDate(LocalDate date) {
        return new CreatePurchaseOrderCommand(
                "EMP-001", "VD-001", "HQ-SE-01", date, null, List.of(),
                PurchaseOrderStatus.DRAFT
        );
    }
}
