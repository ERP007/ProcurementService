package org.fallguys.procurementservice.adapter.outbound.persistence.pendingstatuschange;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.outbound.port.PendingStatusChangePort;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.CancellationPayload;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PendingStatusChange;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ReceivingPayload;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.StatusChangePayload;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PendingStatusChangePersistenceAdapter implements PendingStatusChangePort {

    private final PendingStatusChangeJpaDao jpaDao;
    private final ObjectMapper objectMapper;

    @Override
    public void save(PendingStatusChange pending) {
        String payloadJson = serialize(pending.payload());
        jpaDao.save(PendingStatusChangeEntity.from(pending, payloadJson));
    }

    @Override
    public Optional<PendingStatusChange> findByCode(String poCode) {
        return jpaDao.findById(poCode)
                .map(entity -> entity.toDomain(deserialize(entity.getStatus(), entity.getPayload())));
    }

    @Override
    public void removeByCode(String poCode) {
        jpaDao.deleteById(poCode);
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
