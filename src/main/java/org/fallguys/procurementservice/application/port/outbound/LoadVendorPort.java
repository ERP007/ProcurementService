package org.fallguys.procurementservice.application.port.outbound;

import org.fallguys.procurementservice.domain.model.Vendor;

import java.util.List;
import java.util.Optional;

public interface LoadVendorPort {
    List<Vendor> findAllActiveByNameContaining(String search);
    Optional<Vendor> findActiveByCode(String code);
}
