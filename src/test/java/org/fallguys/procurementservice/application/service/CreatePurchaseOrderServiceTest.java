package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.inbound.command.CreatePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.command.PurchaseOrderLineCommand;
import org.fallguys.procurementservice.application.port.outbound.model.ItemInfo;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivity;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivityType;
import org.fallguys.procurementservice.application.port.outbound.model.WarehouseInfo;
import org.fallguys.procurementservice.application.port.outbound.port.*;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.*;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.purchaseorderline.PurchaseOrderLine;
import org.fallguys.procurementservice.domain.model.vendor.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    @Mock private LoadWarehouseInfoPort loadWarehouseInfoPort;
    @Mock private LoadItemPort loadItemPort;
    @Mock private GeneratePoCodePort generatePoCodePort;
    @Mock private SavePurchaseOrderPort savePurchaseOrderPort;
    @Mock private SavePurchaseOrderStatusHistoryPort savePurchaseOrderStatusHistoryPort;
    @Mock private PublishUserActivityPort publishUserActivityPort;

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
        assertThat(line.lineAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(84000));
    }

    @Test
    void 초안_저장_시_DRAFT_이력만_기록됨() {
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-01");
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.create(UserRole.ADMIN, draftCommandWithoutLines());

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(savePurchaseOrderPort).save(captor.capture());
        assertThat(captor.getValue().getCreation().createdBy()).isEqualTo("EMP-001");

        ArgumentCaptor<PurchaseOrderStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(PurchaseOrderStatusHistory.class);
        verify(savePurchaseOrderStatusHistoryPort).append(historyCaptor.capture());

        PurchaseOrderStatusHistory history = historyCaptor.getValue();
        assertThat(history.poCode()).isEqualTo("PO-2026-06-0001");
        assertThat(history.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(history.actor().code()).isEqualTo("EMP-001");
        assertThat(history.payload()).isNull();

        ArgumentCaptor<UserActivity> activityCaptor = ArgumentCaptor.forClass(UserActivity.class);
        verify(publishUserActivityPort).publish(activityCaptor.capture());
        UserActivity activity = activityCaptor.getValue();
        assertThat(activity.type()).isEqualTo(UserActivityType.CREATED);
        assertThat(activity.status()).isEqualTo("임시저장");
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
        given(loadWarehouseInfoPort.findByCode("HQ-SE-01")).willReturn(new WarehouseInfo("HQ-SE-01", "서울창고"));
        given(loadItemPort.loadAll(List.of("SKU-001"))).willReturn(Map.of("SKU-001", itemInfo));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.create(UserRole.ADMIN, approvedCommandWithLines(List.of(lineCmd)));

        PurchaseOrderLine line = result.getLines().get(0);
        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(line.getItemNameSnapshot()).isEqualTo("브레이크 패드");
        assertThat(line.getUnitSnapshot()).isEqualTo("EA");
        assertThat(line.lineAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    void APPROVED_생성_성공_APPROVED_이력만_기록됨() {
        PurchaseOrderLineCommand lineCmd = new PurchaseOrderLineCommand("SKU-001", 1, BigDecimal.valueOf(1000));
        ItemInfo itemInfo = new ItemInfo("SKU-001", "브레이크 패드", "EA");

        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        willDoNothing().given(loadWarehousePort).verifyActive("HQ-SE-01");
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(loadWarehouseInfoPort.findByCode("HQ-SE-01")).willReturn(new WarehouseInfo("HQ-SE-01", "서울창고"));
        given(loadItemPort.loadAll(List.of("SKU-001"))).willReturn(Map.of("SKU-001", itemInfo));
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.create(UserRole.ADMIN, approvedCommandWithLines(List.of(lineCmd)));

        ArgumentCaptor<PurchaseOrderStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(PurchaseOrderStatusHistory.class);
        verify(savePurchaseOrderStatusHistoryPort).append(historyCaptor.capture());

        PurchaseOrderStatusHistory history = historyCaptor.getValue();
        assertThat(history.poCode()).isEqualTo("PO-2026-06-0001");
        assertThat(history.status()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(history.actor().code()).isEqualTo("EMP-001");

        ArgumentCaptor<UserActivity> activityCaptor = ArgumentCaptor.forClass(UserActivity.class);
        verify(publishUserActivityPort).publish(activityCaptor.capture());
        assertThat(activityCaptor.getValue().status()).isEqualTo("출고대기");
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────

    private CreatePurchaseOrderCommand draftCommandWithoutLines() {
        return draftCommandWithLines(List.of());
    }

    private CreatePurchaseOrderCommand draftCommandWithLines(List<PurchaseOrderLineCommand> lines) {
        return new CreatePurchaseOrderCommand(
                "EMP-001", "홍길동", "사원", "VD-001", "HQ-SE-01",
                "정기 발주 건", lines,
                PurchaseOrderStatus.DRAFT
        );
    }

    private CreatePurchaseOrderCommand approvedCommandWithLines(List<PurchaseOrderLineCommand> lines) {
        return new CreatePurchaseOrderCommand(
                "EMP-001", "홍길동", "사원", "VD-001", "HQ-SE-01",
                "정기 발주 건", lines,
                PurchaseOrderStatus.APPROVED
        );
    }
}
