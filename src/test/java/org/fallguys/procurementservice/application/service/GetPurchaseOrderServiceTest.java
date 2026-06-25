package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.inbound.model.GetPurchaseOrderResult;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderStatusHistoriesPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadItemPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadVendorPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadWarehouseInfoPort;
import org.fallguys.procurementservice.application.port.outbound.model.ItemInfo;
import org.fallguys.procurementservice.application.port.outbound.model.WarehouseInfo;
import org.fallguys.procurementservice.domain.model.purchaseorderline.PurchaseOrderLine;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.purchaseorder.ProcurementOrderCreation;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorder.VendorRef;
import org.fallguys.procurementservice.domain.model.purchaseorder.WarehouseRef;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ActorRef;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.fallguys.procurementservice.domain.model.vendor.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetPurchaseOrderServiceTest {

    @Mock private LoadPurchaseOrderPort loadPurchaseOrderPort;
    @Mock private LoadPurchaseOrderStatusHistoriesPort loadPurchaseOrderStatusHistoriesPort;
    @Mock private LoadVendorPort loadVendorPort;
    @Mock private LoadWarehouseInfoPort loadWarehouseInfoPort;
    @Mock private LoadItemPort loadItemPort;

    @InjectMocks
    private GetPurchaseOrderService service;

    private PurchaseOrder approvedPo;
    private PurchaseOrder draftPo;
    private Vendor vendor;
    private WarehouseInfo warehouseInfo;

    @BeforeEach
    void setUp() {
        ProcurementOrderCreation creation = new ProcurementOrderCreation("EMP-001", Instant.parse("2026-05-18T07:45:00Z"));

        // 확정 건은 공급사·창고명이 박제(snapshot)되어 외부 조회 없이 그대로 표시된다.
        approvedPo = new PurchaseOrder(
                "PO-2026-05-0001",
                VendorRef.snapshot("VD-01", "㈜동성정밀"),
                WarehouseRef.snapshot("WD-01", "본사 중앙창고"),
                PurchaseOrderStatus.APPROVED,
                null,
                List.of(),
                Money.of(BigDecimal.valueOf(14820000)),
                creation
        );

        // DRAFT는 미확정 → code만 보관, 조회 시 master를 live로 채운다.
        draftPo = new PurchaseOrder(
                "PO-2026-05-0002",
                VendorRef.codeOnly("VD-01"),
                WarehouseRef.codeOnly("WD-01"),
                PurchaseOrderStatus.DRAFT,
                null,
                List.of(),
                Money.of(BigDecimal.ZERO),
                creation
        );

        vendor = new Vendor("VD-01", "㈜동성정밀", "홍길동", "010-0000-0000", "서울시", true);
        warehouseInfo = new WarehouseInfo("WD-01", "본사 중앙창고");
    }

    // ── 역할 검증 ──────────────────────────────────────────────────────────

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.get(UserRole.BRANCH_MANAGER, "PO-2026-05-0001"))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadVendorPort, loadWarehouseInfoPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.get(UserRole.BRANCH_STAFF, "PO-2026-05-0001"))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadPurchaseOrderPort, loadVendorPort, loadWarehouseInfoPort);
    }

    // ── 발주서 조회 ────────────────────────────────────────────────────────

    @Test
    void 발주서_미존재이면_ResourceNotFoundException_발생() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(UserRole.ADMIN, "PO-2026-05-0001"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(loadVendorPort, loadWarehouseInfoPort);
    }

    // ── 공급사 조회 (DRAFT는 master live 조회) ──────────────────────────────

    @Test
    void DRAFT_공급사_미존재이면_공급사명은_null() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0002")).willReturn(Optional.of(draftPo));
        given(loadVendorPort.findByCode("VD-01")).willReturn(Optional.empty());
        given(loadWarehouseInfoPort.findByCode("WD-01")).willReturn(warehouseInfo);

        GetPurchaseOrderResult result = service.get(UserRole.ADMIN, "PO-2026-05-0002");

        assertThat(result.order().getVendor().code()).isEqualTo("VD-01");
        assertThat(result.order().getVendor().nameSnapshot()).isNull();
    }

    // ── 성공 ───────────────────────────────────────────────────────────────

    @Test
    void DRAFT_발주서_조회_성공_approvedBy_null() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0002")).willReturn(Optional.of(draftPo));
        given(loadVendorPort.findByCode("VD-01")).willReturn(Optional.of(vendor));
        given(loadWarehouseInfoPort.findByCode("WD-01")).willReturn(warehouseInfo);

        GetPurchaseOrderResult result = service.get(UserRole.HQ_STAFF, "PO-2026-05-0002");

        assertThat(result.order().getCode()).isEqualTo("PO-2026-05-0002");
        assertThat(result.order().getVendor().code()).isEqualTo("VD-01");
        assertThat(result.order().getVendor().nameSnapshot()).isEqualTo("㈜동성정밀");
        assertThat(result.order().getWarehouse().nameSnapshot()).isEqualTo("본사 중앙창고");
        // APPROVED 이력이 없으므로 승인자는 null.
        assertThat(result.approvedBy()).isNull();
    }

    @Test
    void DRAFT_라인_품목정보는_master_live값으로_채워진다() {
        ProcurementOrderCreation creation = new ProcurementOrderCreation("EMP-001", Instant.parse("2026-05-18T07:45:00Z"));
        PurchaseOrder draftWithLines = new PurchaseOrder(
                "PO-2026-05-0003",
                VendorRef.codeOnly("VD-01"),
                WarehouseRef.codeOnly("WD-01"),
                PurchaseOrderStatus.DRAFT,
                null,
                List.of(new PurchaseOrderLine(null, "SKU-1", null, null, 5, Money.of(BigDecimal.valueOf(1000)))),
                Money.of(BigDecimal.valueOf(5000)),
                creation
        );

        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0003")).willReturn(Optional.of(draftWithLines));
        given(loadVendorPort.findByCode("VD-01")).willReturn(Optional.of(vendor));
        given(loadWarehouseInfoPort.findByCode("WD-01")).willReturn(warehouseInfo);
        given(loadItemPort.loadAll(List.of("SKU-1")))
                .willReturn(Map.of("SKU-1", new ItemInfo("SKU-1", "M6 볼트", "EA")));

        GetPurchaseOrderResult result = service.get(UserRole.HQ_STAFF, "PO-2026-05-0003");

        PurchaseOrderLine line = result.order().getLines().get(0);
        assertThat(line.getItemNameSnapshot()).isEqualTo("M6 볼트");
        assertThat(line.getUnitSnapshot()).isEqualTo("EA");
    }

    @Test
    void 승인된_발주서_조회_성공_승인자는_이력_박제값() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(approvedPo));
        given(loadPurchaseOrderStatusHistoriesPort.findLatestByPoCodeAndStatus("PO-2026-05-0001", PurchaseOrderStatus.APPROVED))
                .willReturn(Optional.of(
                        new PurchaseOrderStatusHistory("PO-2026-05-0001", PurchaseOrderStatus.APPROVED,
                                new ActorRef("EMP-002", "이영희", "과장"), null,
                                Instant.parse("2026-05-18T09:00:00Z"))
                ));

        GetPurchaseOrderResult result = service.get(UserRole.HQ_MANAGER, "PO-2026-05-0001");

        // 확정 건은 공급사·창고명을 박제 snapshot으로 쓰므로 master를 조회하지 않는다.
        assertThat(result.order().getStatus()).isEqualTo(PurchaseOrderStatus.APPROVED);
        assertThat(result.order().getVendor().nameSnapshot()).isEqualTo("㈜동성정밀");
        assertThat(result.order().getWarehouse().nameSnapshot()).isEqualTo("본사 중앙창고");
        assertThat(result.approvedBy().code()).isEqualTo("EMP-002");
        assertThat(result.approvedBy().name()).isEqualTo("이영희");
        assertThat(result.approvedBy().position()).isEqualTo("과장");
    }

    @Test
    void 승인된_발주서_APPROVED_이력_없으면_approvedBy_null() {
        given(loadPurchaseOrderPort.findByCode("PO-2026-05-0001")).willReturn(Optional.of(approvedPo));
        given(loadPurchaseOrderStatusHistoriesPort.findLatestByPoCodeAndStatus("PO-2026-05-0001", PurchaseOrderStatus.APPROVED))
                .willReturn(Optional.empty());

        GetPurchaseOrderResult result = service.get(UserRole.ADMIN, "PO-2026-05-0001");

        assertThat(result.approvedBy()).isNull();
    }
}
