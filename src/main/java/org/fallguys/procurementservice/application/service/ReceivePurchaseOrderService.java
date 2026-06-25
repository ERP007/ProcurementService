package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.command.ReceivePurchaseOrderCommand;
import org.fallguys.procurementservice.application.port.inbound.usecase.ReceivePurchaseOrderUseCase;
import org.fallguys.procurementservice.application.port.outbound.model.Executor;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivity;
import org.fallguys.procurementservice.application.port.outbound.model.UserActivityType;
import org.fallguys.procurementservice.application.port.outbound.port.InboundStockPort;
import org.fallguys.procurementservice.application.port.outbound.port.PublishUserActivityPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadWarehousePort;
import org.fallguys.procurementservice.application.port.outbound.port.PendingStatusChangePort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderStatusHistoryPort;
import org.fallguys.procurementservice.application.port.outbound.port.SyncInboundStockPort;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ActorRef;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PendingStatusChange;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ReceivingPayload;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReceivePurchaseOrderService implements ReceivePurchaseOrderUseCase {

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final LoadWarehousePort loadWarehousePort;
    private final InboundStockPort inboundStockPort;
    private final SyncInboundStockPort syncInboundStockPort;
    private final SavePurchaseOrderPort savePurchaseOrderPort;
    private final PendingStatusChangePort pendingStatusChangePort;
    private final SavePurchaseOrderStatusHistoryPort savePurchaseOrderStatusHistoryPort;
    private final PublishUserActivityPort publishUserActivityPort;

    // 재고 입고 처리 방식. true=동기 REST(부하 테스트용), false=outbox 기반 async(기본).
    @Value("${stock.sync-mode}")
    private boolean stockSyncMode;

    /**
     * 발주서를 전량 입고 처리한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) code로 발주서를 조회한다(없으면 404).
     * 3) 창고 활성 여부를 검증한다(비활성이면 400).
     * 4) 도메인 receive() 호출: APPROVED 가드 + saga 진행 중 가드 + 상태 provisional 전환(saga SENDING).
     * 5) 재고 서비스에 입고 처리를 요청한다.
     * 6) 발주서 저장 + 상태 전환을 staging에 적재한다.
     *    RECEIVED는 saga로 보상될 수 있는 provisional 상태이므로 이력에 즉시 박지 않고,
     *    saga DONE 확정 시 이력으로 승격한다. 행위 시점(now)은 staging에 보존된다.
     *
     * 트랜잭션: 쓰기. 재고 서비스 호출은 트랜잭션 내에서 수행되며, 실패 시 예외 전파로 PO 상태 변경·staging이 롤백된다.
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

        loadWarehousePort.verifyActive(order.getWarehouse().code());

        order.receive();

        PurchaseOrder saved = savePurchaseOrderPort.save(order);

        PendingStatusChange pending = new PendingStatusChange(
                saved.getCode(),
                PurchaseOrderStatus.RECEIVED,
                new ActorRef(command.userCode(), command.userName(), command.userPosition()),
                new ReceivingPayload(command.receivedDate()),
                Instant.now()
        );

        if (stockSyncMode) {
            // sync 경로(부하 테스트): 같은 트랜잭션 안에서 재고 입고를 동기 REST로 호출한다.
            // 실패 시 예외가 전파돼 상태 전환까지 전부 롤백된다(provisional 미잔존).
            // 성공하면 reply를 기다리지 않고 즉시 saga를 DONE으로 확정하고 이력을 기록한다
            // (CompleteSagaService와 동일한 확정 로직을 인라인으로 수행).
            syncInboundStockPort.inbound(saved);
            saved.markSagaProcessing();
            saved.markSagaDone();
            savePurchaseOrderPort.save(saved);
            savePurchaseOrderStatusHistoryPort.append(pending.toHistory());

            // 사용자 활동: 입고 확정은 sync 경로에서 즉시 발생. async는 CompleteSagaService가 발행한다.
            publishUserActivityPort.publish(new UserActivity(
                    command.userCode(), UserActivityType.RECEIVED,
                    saved.getCode(), saved.getVendor().nameSnapshot(), pending.occurredAt()));
        } else {
            // async 경로(기본): 입고 saga가 DONE으로 확정돼야 이력에 남는다. 지금은 행위자·수령
            // 부가 데이터를 staging에만 보관하고, reply 성공 수신 시 이력으로 승격한다(실패 시 폐기).
            inboundStockPort.inbound(saved, new Executor(command.userCode(), command.userName()));
            pendingStatusChangePort.save(pending);
        }

        return saved;
    }
}
