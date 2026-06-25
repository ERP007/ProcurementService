package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.model.GetPurchaseOrderResult;
import org.fallguys.procurementservice.application.port.inbound.usecase.GetPurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.outbound.model.ItemInfo;
import org.fallguys.procurementservice.application.port.outbound.port.LoadItemPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderStatusHistoriesPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadVendorPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadWarehouseInfoPort;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ActorRef;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.fallguys.procurementservice.domain.model.purchaseorderline.PurchaseOrderLine;
import org.fallguys.procurementservice.domain.model.vendor.Vendor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetPurchaseOrderService implements GetPurchaseOrderUseCase {

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final LoadPurchaseOrderStatusHistoriesPort loadPurchaseOrderStatusHistoriesPort;
    private final LoadVendorPort loadVendorPort;
    private final LoadWarehouseInfoPort loadWarehouseInfoPort;
    private final LoadItemPort loadItemPort;

    /**
     * 발주서 단건을 조회한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) code로 발주서를 조회한다(없으면 404).
     * 3) 공급사명 결정:
     *    - DRAFT(미확정): 박제값이 없으므로 공급사 master를 live 조회해 채운다(없으면 null).
     *    - 확정(APPROVED 이후): 박제된 snapshot을 그대로 쓴다(외부 호출 0, 당시 값 보존).
     * 4) warehouseCode로 창고 정보를 조회한다(재고 서비스 호출).
     * 5) 라인 품목명·단위 결정:
     *    - DRAFT(미확정): 라인 스냅샷이 비어 있으므로 SKU로 품목 master를 live 조회해 채운다.
     *    - 확정(APPROVED 이후): 라인에 박제된 snapshot을 그대로 쓴다(외부 호출 0).
     * 6) 결재자(approvedBy)가 있으면 유저 서비스에서 유저 정보를 조회한다.
     *
     * 트랜잭션: 읽기 전용. live로 채운 표시값은 응답 enrich 용도이며 DB에 다시 저장하지 않는다.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 발주서 미존재: ResourceNotFoundException (PO-02-04, 404)
     * - 품목 미존재·비활성(DRAFT live 조회 시): ResourceNotFoundException (PO-02-03, 404) / BusinessValidationException (PO-03-03, 400)
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

        // DRAFT는 vendor·warehouse·라인 표시명이 비어 있으므로 master를 live 조회해 order 도메인에 채운다(영속화 X).
        // 확정 건은 박제 snapshot을 그대로 쓰므로 외부 호출 0.
        enrichDraftSnapshots(order);

        // 승인자는 APPROVED 이력에 박제된 행위자 스냅샷을 그대로 쓴다(외부 호출 0).
        ActorRef approvedBy = findApprover(code).orElse(null);

        return new GetPurchaseOrderResult(order, approvedBy);
    }

    // DRAFT만 master를 live 조회해 order 도메인의 표시명을 채운다. 확정 건은 박제 snapshot이라 그대로 둔다.
    private void enrichDraftSnapshots(PurchaseOrder order) {
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            return;
        }
        // 공급사 master가 없으면 표시명은 null로 둔다(조회 실패로 보지 않음).
        String vendorName = loadVendorPort.findByCode(order.getVendor().code())
                .map(Vendor::getName)
                .orElse(null);
        order.enrichVendorName(vendorName);

        order.enrichWarehouseName(loadWarehouseInfoPort.findByCode(order.getWarehouse().code()).name());

        enrichLineSnapshots(order);
    }

    // 라인 스냅샷이 비어 있으므로 SKU별 품목 master를 live 조회해 도메인 라인에 채운다.
    private void enrichLineSnapshots(PurchaseOrder order) {
        if (order.getLines().isEmpty()) {
            return;
        }
        List<String> skus = order.getLines().stream()
                .map(PurchaseOrderLine::getItemSku)
                .toList();
        Map<String, ItemInfo> itemInfos = loadItemPort.loadAll(skus);
        order.getLines().forEach(line -> {
            ItemInfo info = itemInfos.get(line.getItemSku());
            if (info != null) {
                line.enrichSnapshot(info.itemName(), info.unit());
            }
        });
    }

    // 승인자는 가장 최근 APPROVED 이력의 박제된 행위자다(전체 이력 로드 없이 code+status로 1건만 조회).
    private Optional<ActorRef> findApprover(String code) {
        return loadPurchaseOrderStatusHistoriesPort
                .findLatestByPoCodeAndStatus(code, PurchaseOrderStatus.APPROVED)
                .map(PurchaseOrderStatusHistory::actor);
    }
}
