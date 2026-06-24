package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.domain.model.vendor.Vendor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadVendorPort {
    List<Vendor> findAllActiveByNameContaining(String search);
    Optional<Vendor> findActiveByCode(String code);
    Optional<Vendor> findByCode(String code);

    // 목록 enrich용 배치 조회(DRAFT 건 공급사명 채우기). active 무관, code(PK) 기준.
    List<Vendor> findAllByCodeIn(Collection<String> codes);
}
