package org.fallguys.procurementservice.application.port.outbound;

import org.fallguys.procurementservice.domain.model.Vendor;

import java.util.List;

public interface LoadVendorPort {
    List<Vendor> findAllActiveByNameContaining(String search);
}
