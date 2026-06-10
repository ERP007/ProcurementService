package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.inbound.GetPurchaseOrderResult;
import org.fallguys.procurementservice.application.port.outbound.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.LoadUserPort;
import org.fallguys.procurementservice.application.port.outbound.LoadVendorPort;
import org.fallguys.procurementservice.application.port.outbound.LoadWarehouseInfoPort;
import org.fallguys.procurementservice.application.port.outbound.UserInfo;
import org.fallguys.procurementservice.application.port.outbound.WarehouseInfo;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.ProcurementOrderApproval;
import org.fallguys.procurementservice.domain.model.ProcurementOrderCreation;
import org.fallguys.procurementservice.domain.model.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.fallguys.procurementservice.domain.model.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetPurchaseOrderServiceTest {

    @Mock private LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock private LoadVendorPort loadVendorPort;
    @Mock private LoadWarehouseInfoPort loadWarehouseInfoPort;
    @Mock private LoadUserPort loadUserPort;

    @InjectMocks
    private GetPurchaseOrderService service;

    private PurchaseOrder approvedPo;
    private PurchaseOrder draftPo;
    private Vendor vendor;
    private WarehouseInfo warehouseInfo;
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        ProcurementOrderCreation creation = new ProcurementOrderCreation("EMP-001", Instant.parse("2026-05-18T07:45:00Z"));
        ProcurementOrderApproval approval = new ProcurementOrderApproval("EMP-002", Instant.parse("2026-05-18T09:00:00Z"));

        approvedPo = new PurchaseOrder(
                "PO-2026-05-0001", "VD-01", "WD-01",
                PurchaseOrderStatus.APPROVED,
                LocalDate.of(2026, 5, 24), null,
                List.of(),
                Money.of(BigDecimal.valueOf(14820000)),
                creation, approval, null, null
        );

        draftPo = new PurchaseOrder(
                "PO-2026-05-0002", "VD-01", "WD-01",
                PurchaseOrderStatus.DRAFT,
                LocalDate.of(2026, 5, 24), null,
                List.of(),
                Money.of(BigDecimal.ZERO),
                creation, null, null, null
        );

        vendor = new Vendor("VD-01", "㈜동성정밀", "홍길동", "010-0000-0000", "서울시", true);
        warehouseInfo = new WarehouseInfo("WD-01", "본사 중앙창고");
        userInfo = new UserInfo("EMP-002", "이영희", "과장");
    }

    // ── 역할 검증 ──────────────────────────────────────────────────────────

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.get(UserRole.BRANCH_MANAGER, "PO-2026-05-0001"))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadVendorPort, loadWarehouseInfoPort, loadUserPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.get(UserRole.BRANCH_STAFF, "PO-2026-05-0001"))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadVendorPort, loadWarehouseInfoPort, loadUserPort);
    }

    // ── 발주서 조회 ────────────────────────────────────────────────────────

    @Test
    void 발주서_미존재이면_ResourceNotFoundException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(UserRole.ADMIN, "PO-2026-05-0001"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadVendorPort, loadWarehouseInfoPort, loadUserPort);
    }

    // ── 공급사 조회 ────────────────────────────────────────────────────────

    @Test
    void 공급사_미존재이면_ResourceNotFoundException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(approvedPo));
        given(loadVendorPort.findByCode("VD-01")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(UserRole.ADMIN, "PO-2026-05-0001"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadWarehouseInfoPort, loadUserPort);
    }

    // ── 성공 ───────────────────────────────────────────────────────────────

    @Test
    void DRAFT_발주서_조회_성공_approvedByUser_null() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0002")).willReturn(Optional.of(draftPo));
        given(loadVendorPort.findByCode("VD-01")).willReturn(Optional.of(vendor));
        given(loadWarehouseInfoPort.findByCode("WD-01")).willReturn(warehouseInfo);

        GetPurchaseOrderResult result = service.get(UserRole.HQ_STAFF, "PO-2026-05-0002");

        assertThat(result.order().getCode()).isEqualTo("PO-2026-05-0002");
        assertThat(result.vendor().getCode()).isEqualTo("VD-01");
        assertThat(result.warehouse().name()).isEqualTo("본사 중앙창고");
        assertThat(result.approvedByUser()).isNull();

        verifyNoInteractions(loadUserPort);
    }

    @Test
    void 승인된_발주서_조회_성공_유저_정보_포함() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(approvedPo));
        given(loadVendorPort.findByCode("VD-01")).willReturn(Optional.of(vendor));
        given(loadWarehouseInfoPort.findByCode("WD-01")).willReturn(warehouseInfo);
        given(loadUserPort.findByCode("EMP-002")).willReturn(Optional.of(userInfo));

        GetPurchaseOrderResult result = service.get(UserRole.HQ_MANAGER, "PO-2026-05-0001");

        assertThat(result.order().getStatus()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(result.approvedByUser().code()).isEqualTo("EMP-002");
        assertThat(result.approvedByUser().name()).isEqualTo("이영희");
        assertThat(result.approvedByUser().position()).isEqualTo("과장");
    }

    @Test
    void 승인된_발주서_조회_유저_미존재이면_approvedByUser_null() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(approvedPo));
        given(loadVendorPort.findByCode("VD-01")).willReturn(Optional.of(vendor));
        given(loadWarehouseInfoPort.findByCode("WD-01")).willReturn(warehouseInfo);
        given(loadUserPort.findByCode("EMP-002")).willReturn(Optional.empty());

        GetPurchaseOrderResult result = service.get(UserRole.ADMIN, "PO-2026-05-0001");

        assertThat(result.approvedByUser()).isNull();
    }
}
