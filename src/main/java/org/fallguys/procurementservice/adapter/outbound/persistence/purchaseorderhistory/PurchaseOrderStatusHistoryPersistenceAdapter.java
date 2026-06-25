package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorderhistory;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderStatusHistoriesPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderStatusHistoryPort;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.CancellationPayload;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PurchaseOrderStatusHistory;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ReceivingPayload;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.StatusChangePayload;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PurchaseOrderStatusHistoryPersistenceAdapter
        implements SavePurchaseOrderStatusHistoryPort, LoadPurchaseOrderStatusHistoriesPort {

    private final PurchaseOrderStatusHistoryJpaDao jpaDao;
    private final ObjectMapper objectMapper;

    @Override
    public void append(PurchaseOrderStatusHistory history) {
        String payloadJson = serialize(history.payload());
        jpaDao.save(PurchaseOrderStatusHistoryEntity.from(history, payloadJson));
    }

    @Override
    public List<PurchaseOrderStatusHistory> findByPoCode(String poCode) {
        return jpaDao.findByPoCodeOrderByCreatedAtDesc(poCode).stream()
                .map(entity -> entity.toDomain(deserialize(entity.getStatus(), entity.getPayload())))
                .toList();
    }

    @Override
    public Optional<PurchaseOrderStatusHistory> findLatestByPoCodeAndStatus(String poCode, PurchaseOrderStatus status) {
        return jpaDao.findFirstByPoCodeAndStatusOrderByCreatedAtDesc(poCode, status)
                .map(entity -> entity.toDomain(deserialize(entity.getStatus(), entity.getPayload())));
    }

    // payload 도메인 → JSON 문자열. 부가 데이터가 없는 전이는 null.
    private String serialize(StatusChangePayload payload) {
        if (payload == null) return null;
        return objectMapper.writeValueAsString(payload);
    }

    // 상태별로 payload JSON을 알맞은 구현체로 역직렬화. 부가 데이터가 없으면 null.
    private StatusChangePayload deserialize(PurchaseOrderStatus status, String payloadJson) {
        if (payloadJson == null) return null;
        return switch (status) {
            case RECEIVED -> objectMapper.readValue(payloadJson, ReceivingPayload.class);
            case CANCELED -> objectMapper.readValue(payloadJson, CancellationPayload.class);
            case DRAFT, APPROVED -> null;
        };
    }
}
