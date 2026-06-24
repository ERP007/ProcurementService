package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorderhistory;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ActorRef;

/**
 * 행위자 참조 매핑. 행위 시점에 박제한 code·name·position을 보관한다.
 * 이력(history)과 staging(pending) 엔티티가 공유한다.
 */
@Embeddable
public record ActorRefEmbeddable(

        @Column(name = "actor_code", nullable = false)
        String code,

        @Column(name = "actor_name_snapshot")
        String nameSnapshot,

        @Column(name = "actor_position_snapshot")
        String positionSnapshot

) {
    public ActorRef toDomain() {
        return new ActorRef(code, nameSnapshot, positionSnapshot);
    }

    public static ActorRefEmbeddable from(ActorRef ref) {
        return new ActorRefEmbeddable(ref.code(), ref.name(), ref.position());
    }
}
