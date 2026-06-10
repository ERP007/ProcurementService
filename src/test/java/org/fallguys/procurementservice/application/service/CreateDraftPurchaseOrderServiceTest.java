package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.inbound.CreateDraftPurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.CreatePurchaseOrderLineCommand;
import org.fallguys.procurementservice.application.port.outbound.*;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CreateDraftPurchaseOrderServiceTest {

    @Mock private LoadVendorPort loadVendorPort;
    @Mock private LoadWarehousePort loadWarehousePort;
    @Mock private LoadItemPort loadItemPort;
    @Mock private GeneratePoCodePort generatePoCodePort;
    @Mock private SavePurchaseOrderPort savePurchaseOrderPort;

    @InjectMocks
    private CreateDraftPurchaseOrderService service;

    private Vendor vendor;
    private ItemInfo itemInfo;

    @BeforeEach
    void setUp() {
        vendor = new Vendor("VD-001", "㈜동성정밀", "김담당", "010-1234-5678", "서울시", true);
        itemInfo = new ItemInfo("SKU-001", "엔진오일필터", "EA");
    }

    // ── 역할 검증 ──────────────────────────────────────────────────────────

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        CreateDraftPurchaseOrderCommand command = validCommandWithoutLines();

        assertThatThrownBy(() -> service.createDraft(UserRole.BRANCH_MANAGER, command))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadVendorPort, loadWarehousePort, loadItemPort, generatePoCodePort, savePurchaseOrderPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        CreateDraftPurchaseOrderCommand command = validCommandWithoutLines();

        assertThatThrownBy(() -> service.createDraft(UserRole.BRANCH_STAFF, command))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadVendorPort, loadWarehousePort, loadItemPort, generatePoCodePort, savePurchaseOrderPort);
    }

    // ── 도착 희망일 검증 ───────────────────────────────────────────────────

    @Test
    void 도착희망일이_1년_초과이면_BusinessValidationException_발생() {
        LocalDate overOneYear = LocalDate.now().plusYears(1).plusDays(1);
        CreateDraftPurchaseOrderCommand command = commandWithDate(overOneYear);

        assertThatThrownBy(() -> service.createDraft(UserRole.ADMIN, command))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(loadVendorPort, loadWarehousePort, loadItemPort, generatePoCodePort, savePurchaseOrderPort);
    }

    @Test
    void 도착희망일이_정확히_1년이면_통과() {
        LocalDate exactlyOneYear = LocalDate.now().plusYears(1);
        CreateDraftPurchaseOrderCommand command = commandWithDate(exactlyOneYear);

        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        given(loadWarehousePort.existsByCode("HQ-SE-01")).willReturn(true);
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        assertThat(service.createDraft(UserRole.ADMIN, command)).isNotNull();
    }

    // ── 중복 품목 검증 ─────────────────────────────────────────────────────

    @Test
    void 동일_itemSku_중복이면_BusinessValidationException_발생() {
        CreatePurchaseOrderLineCommand line = new CreatePurchaseOrderLineCommand("SKU-001", 10, BigDecimal.valueOf(8400));
        CreateDraftPurchaseOrderCommand command = commandWithLines(List.of(line, line));

        assertThatThrownBy(() -> service.createDraft(UserRole.ADMIN, command))
                .isInstanceOf(BusinessValidationException.class);

        verifyNoInteractions(loadVendorPort, loadWarehousePort, loadItemPort, generatePoCodePort, savePurchaseOrderPort);
    }

    // ── 공급사·창고·품목 조회 ───────────────────────────────────────────────

    @Test
    void 공급사_미존재이면_ResourceNotFoundException_발생() {
        CreateDraftPurchaseOrderCommand command = validCommandWithoutLines();
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createDraft(UserRole.ADMIN, command))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadWarehousePort, loadItemPort, generatePoCodePort, savePurchaseOrderPort);
    }

    @Test
    void 창고_미존재이면_ResourceNotFoundException_발생() {
        CreateDraftPurchaseOrderCommand command = validCommandWithoutLines();
        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        given(loadWarehousePort.existsByCode("HQ-SE-01")).willReturn(false);

        assertThatThrownBy(() -> service.createDraft(UserRole.ADMIN, command))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadItemPort, generatePoCodePort, savePurchaseOrderPort);
    }

    @Test
    void 품목_미존재이면_ResourceNotFoundException_발생() {
        CreatePurchaseOrderLineCommand line = new CreatePurchaseOrderLineCommand("SKU-999", 10, BigDecimal.valueOf(8400));
        CreateDraftPurchaseOrderCommand command = commandWithLines(List.of(line));

        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        given(loadWarehousePort.existsByCode("HQ-SE-01")).willReturn(true);
        given(loadItemPort.loadAll(List.of("SKU-999"))).willReturn(Map.of());

        assertThatThrownBy(() -> service.createDraft(UserRole.ADMIN, command))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(generatePoCodePort, savePurchaseOrderPort);
    }

    // ── 성공 ───────────────────────────────────────────────────────────────

    @Test
    void 라인_없이_초안_생성_성공() {
        CreateDraftPurchaseOrderCommand command = validCommandWithoutLines();

        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        given(loadWarehousePort.existsByCode("HQ-SE-01")).willReturn(true);
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.createDraft(UserRole.HQ_MANAGER, command);

        assertThat(result.getCode()).isEqualTo("PO-2026-06-0001");
        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(result.getLines()).isEmpty();
        verifyNoInteractions(loadItemPort);
    }

    @Test
    void 라인_포함_초안_생성_성공() {
        CreatePurchaseOrderLineCommand lineCmd = new CreatePurchaseOrderLineCommand("SKU-001", 10, BigDecimal.valueOf(8400));
        CreateDraftPurchaseOrderCommand command = commandWithLines(List.of(lineCmd));

        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        given(loadWarehousePort.existsByCode("HQ-SE-01")).willReturn(true);
        given(loadItemPort.loadAll(List.of("SKU-001"))).willReturn(Map.of("SKU-001", itemInfo));
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.createDraft(UserRole.HQ_STAFF, command);

        assertThat(result.getCode()).isEqualTo("PO-2026-06-0001");
        assertThat(result.getVendorCode()).isEqualTo("VD-001");
        assertThat(result.getWarehouseCode()).isEqualTo("HQ-SE-01");
        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);

        PurchaseOrderLine line = result.getLines().get(0);
        assertThat(line.getItemSku()).isEqualTo("SKU-001");
        assertThat(line.getItemNameSnapshot()).isEqualTo("엔진오일필터");
        assertThat(line.getOrderQuantity()).isEqualTo(10);
        assertThat(line.getLineAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(84000));
    }

    @Test
    void 저장_시_올바른_도메인_객체가_전달된다() {
        CreateDraftPurchaseOrderCommand command = validCommandWithoutLines();

        given(loadVendorPort.findActiveByCode("VD-001")).willReturn(Optional.of(vendor));
        given(loadWarehousePort.existsByCode("HQ-SE-01")).willReturn(true);
        given(generatePoCodePort.generate()).willReturn("PO-2026-06-0001");
        given(savePurchaseOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.createDraft(UserRole.ADMIN, command);

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(savePurchaseOrderPort).save(captor.capture());

        PurchaseOrder saved = captor.getValue();
        assertThat(saved.getCreation().createdBy()).isEqualTo("EMP-001");
        assertThat(saved.getApproval()).isNull();
        assertThat(saved.getReceiving()).isNull();
        assertThat(saved.getCancellation()).isNull();
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────

    private CreateDraftPurchaseOrderCommand validCommandWithoutLines() {
        return commandWithLines(List.of());
    }

    private CreateDraftPurchaseOrderCommand commandWithLines(List<CreatePurchaseOrderLineCommand> lines) {
        return new CreateDraftPurchaseOrderCommand(
                "EMP-001",
                "VD-001",
                "HQ-SE-01",
                LocalDate.now().plusDays(7),
                "정기 발주 건",
                lines
        );
    }

    private CreateDraftPurchaseOrderCommand commandWithDate(LocalDate date) {
        return new CreateDraftPurchaseOrderCommand(
                "EMP-001",
                "VD-001",
                "HQ-SE-01",
                date,
                null,
                List.of()
        );
    }
}
