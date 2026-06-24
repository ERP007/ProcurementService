package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorder;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.fallguys.procurementservice.domain.model.purchaseorder.VendorRef;

/**
 * 공급사 참조 매핑. master FK 없이 code + 확정 시점 박제명만 보관한다.
 * 변환 로직을 이 VO 안에 두어 엔티티 매핑을 단순화한다.
 */
@Embeddable
public record VendorRefEmbeddable(

        @Column(name = "vendor_code", nullable = false)
        String code,

        @Column(name = "vendor_name_snapshot")
        String nameSnapshot

) {
    public VendorRef toDomain() {
        return new VendorRef(code, nameSnapshot);
    }

    public static VendorRefEmbeddable from(VendorRef ref) {
        return new VendorRefEmbeddable(ref.code(), ref.nameSnapshot());
    }
}
