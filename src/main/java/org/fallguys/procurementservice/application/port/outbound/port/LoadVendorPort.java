package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.domain.model.vendor.Vendor;

import java.util.List;
import java.util.Optional;

public interface LoadVendorPort {
    List<Vendor> findAllActiveByNameContaining(String search);
    Optional<Vendor> findActiveByCode(String code);
    Optional<Vendor> findByCode(String code);
}
