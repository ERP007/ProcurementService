package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.domain.model.purchaseorderhistory.PendingStatusChange;

import java.util.Optional;

/**
 * saga in-flight 상태 전환(staging) 저장소 포트.
 * 애그리거트당 동시 진행 saga는 1건이므로 poCode를 키로 다룬다.
 */
public interface PendingStatusChangePort {
    void save(PendingStatusChange pending);

    Optional<PendingStatusChange> findByCode(String poCode);

    void removeByCode(String poCode);
}
