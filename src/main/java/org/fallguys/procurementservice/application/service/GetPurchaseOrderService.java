package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.model.GetPurchaseOrderResult;
import org.fallguys.procurementservice.application.port.inbound.usecase.GetPurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderStatusHistoriesPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadUserPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadVendorPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadWarehouseInfoPort;
import org.fallguys.procurementservice.application.port.outbound.model.UserInfo;
import org.fallguys.procurementservice.application.port.outbound.model.WarehouseInfo;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.fallguys.procurementservice.domain.model.vendor.Vendor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetPurchaseOrderService implements GetPurchaseOrderUseCase {

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final LoadPurchaseOrderStatusHistoriesPort loadPurchaseOrderStatusHistoriesPort;
    private final LoadVendorPort loadVendorPort;
    private final LoadWarehouseInfoPort loadWarehouseInfoPort;
    private final LoadUserPort loadUserPort;

    /**
     * 발주서 단건을 조회한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) code로 발주서를 조회한다(없으면 404).
     * 3) vendorCode로 공급사를 조회한다(비활성 포함, 없으면 404).
     * 4) warehouseCode로 창고 정보를 조회한다(재고 서비스 호출).
     * 5) 결재자(approvedBy)가 있으면 유저 서비스에서 유저 정보를 조회한다.
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 발주서 미존재: ResourceNotFoundException (PO-02-04, 404)
     * - 공급사 미존재: ResourceNotFoundException (PO-02-01, 404)
     * - 창고 조회 실패: ExternalServiceException (PO-07-02, 502)
     * - 유저 조회 실패: ExternalServiceException (PO-07-03, 502)
     */
    @Override
    @Transactional(readOnly = true)
    public GetPurchaseOrderResult get(UserRole role, String code) {
        if (!role.isHqUser()) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }

        PurchaseOrder order = loadPurchaseOrderPort.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND));

        Vendor vendor = loadVendorPort.findByCode(order.getVendorCode())
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.VENDOR_NOT_FOUND_ON_DETAIL));

        WarehouseInfo warehouse = loadWarehouseInfoPort.findByCode(order.getWarehouseCode());

        UserInfo approvedByUser = findApprover(code)
                .flatMap(loadUserPort::findByCode)
                .orElse(null);

        return new GetPurchaseOrderResult(order, vendor, warehouse, approvedByUser);
    }

    // 승인자는 가장 최근 APPROVED 이력의 담당자다(이력은 최신순 정렬).
    private Optional<String> findApprover(String code) {
        return loadPurchaseOrderStatusHistoriesPort.findByPoCode(code).stream()
                .filter(h -> h.status() == PurchaseOrderStatus.APPROVED)
                .map(PurchaseOrderStatusHistory::actorCode)
                .findFirst();
    }
}
