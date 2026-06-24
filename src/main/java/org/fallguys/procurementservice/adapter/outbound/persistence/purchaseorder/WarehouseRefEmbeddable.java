package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorder;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.fallguys.procurementservice.domain.model.purchaseorder.WarehouseRef;

/**
 * 창고 참조 매핑. code + 확정 시점 박제명만 보관한다(타 서비스 소유 master).
 */
@Embeddable
public record WarehouseRefEmbeddable(

        @Column(name = "warehouse_code", nullable = false)
        String code,

        @Column(name = "warehouse_name_snapshot")
        String nameSnapshot

) {
    public WarehouseRef toDomain() {
        return new WarehouseRef(code, nameSnapshot);
    }

    public static WarehouseRefEmbeddable from(WarehouseRef ref) {
        return new WarehouseRefEmbeddable(ref.code(), ref.nameSnapshot());
    }
}
