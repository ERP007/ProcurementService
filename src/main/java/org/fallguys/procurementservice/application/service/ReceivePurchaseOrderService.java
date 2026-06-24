package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.command.ReceivePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.usecase.ReceivePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.outbound.model.Executor;
import org.fallguys.procurementservice.application.port.outbound.port.InboundStockPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadWarehousePort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderStatusHistoryPort;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ReceivingPayload;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReceivePurchaseOrderService implements ReceivePurchaseOrderUseCase {

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final LoadWarehousePort loadWarehousePort;
    private final InboundStockPort inboundStockPort;
    private final SavePurchaseOrderPort savePurchaseOrderPort;
    private final SavePurchaseOrderStatusHistoryPort savePurchaseOrderStatusHistoryPort;

    /**
     * 발주서를 전량 입고 처리한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) code로 발주서를 조회한다(없으면 404).
     * 3) 창고 활성 여부를 검증한다(비활성이면 400).
     * 4) 도메인 receive() 호출: APPROVED 가드 + 상태·receiving 변경.
     * 5) 재고 서비스에 입고 처리를 요청한다.
     * 6) 저장 후 반환한다.
     *
     * 트랜잭션: 쓰기. 재고 서비스 호출은 트랜잭션 내에서 수행되며, 실패 시 예외 전파로 PO 상태 변경이 롤백된다.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 발주서 미존재: ResourceNotFoundException (PO-02-04, 404)
     * - 창고 미존재·비활성: ResourceNotFoundException/BusinessValidationException (PO-02-02/PO-03-04)
     * - APPROVED 아닌 상태: BusinessValidationException (PO-03-06, 400, 도메인이 던짐)
     * - 재고 서비스 실패: ExternalServiceException (PO-07-02, 502, 롤백)
     */
    @Override
    @Transactional
    public PurchaseOrder receive(UserRole role, ReceivePurchaseOrderCommand command) {
        if (!role.isHqUser()) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }

        PurchaseOrder order = loadPurchaseOrderPort.findByCode(command.code())
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND));

        loadWarehousePort.verifyActive(order.getWarehouseCode());

        order.receive();

        inboundStockPort.inbound(order, new Executor(command.userCode(), command.userName()));

        PurchaseOrder saved = savePurchaseOrderPort.save(order);

        savePurchaseOrderStatusHistoryPort.append(new PurchaseOrderStatusHistory(
                saved.getCode(),
                PurchaseOrderStatus.RECEIVED,
                command.userCode(),
                new ReceivingPayload(command.receivedDate()),
                Instant.now()
        ));

        return saved;
    }
}
