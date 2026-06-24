package org.fallguys.procurementservice.domain.model.purchaseorder;

/**
 * 화면 표시용 진행 상태. 비즈니스 마일스톤({@link PurchaseOrderStatus})과 재고 입고 saga 진행
 * ({@link SagaStatus})의 조합을 백엔드가 단일 값으로 파생해 노출한다. UI는 이 enum을
 * 라벨로만 매핑하고, 조합 규칙(어떤 status+saga가 어떤 진행인지)은 알 필요가 없다.
 */
public enum PurchaseOrderProgress {
    DRAFT,
    APPROVED,               // 승인 확정(입고 전 또는 입고 성공 후 상태 유지)
    INBOUND_IN_PROGRESS,    // 입고 saga 진행 중(SENDING/PROCESSING)
    RECEIVED,               // 입고 확정(saga DONE)
    INBOUND_FAILED,         // 입고 보상으로 APPROVED 복귀(재입고 필요)
    CANCELED;

    /** (status, saga) 조합 규칙의 단일 소스. 애그리거트·요약 매핑이 공통으로 사용한다. */
    public static PurchaseOrderProgress from(PurchaseOrderStatus status, SagaStatus saga) {
        return switch (status) {
            case DRAFT -> DRAFT;
            case APPROVED -> switch (saga) {
                case SENDING, PROCESSING -> INBOUND_IN_PROGRESS;
                case FAILED -> INBOUND_FAILED;
                case NONE, DONE -> APPROVED;
            };
            case RECEIVED -> saga.inProgress() ? INBOUND_IN_PROGRESS : RECEIVED;
            case CANCELED -> CANCELED;
        };
    }
}
