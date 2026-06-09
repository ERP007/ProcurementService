package org.fallguys.procurementservice.adapter.outbound.persistence;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.outbound.LoadVendorPort;
import org.fallguys.procurementservice.domain.model.Vendor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VendorPersistenceAdapter implements LoadVendorPort {

    private final VendorJpaDao vendorJpaDao;

    @Override
    public List<Vendor> findAllActiveByNameContaining(String search) {
        if (search == null || search.isBlank()) {
            return vendorJpaDao.findAllByActiveTrue().stream()
                    .map(VendorEntity::toDomain)
                    .toList();
        }
        return vendorJpaDao.findAllByActiveTrueAndNameContaining(search).stream()
                .map(VendorEntity::toDomain)
                .toList();
    }
}
