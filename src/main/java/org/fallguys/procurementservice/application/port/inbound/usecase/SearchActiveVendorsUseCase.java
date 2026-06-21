package org.fallguys.procurementservice.application.port.inbound.usecase;

import org.fallguys.procurementservice.domain.model.UserRole;
import org.fallguys.procurementservice.domain.model.vendor.Vendor;

import java.util.List;

public interface SearchActiveVendorsUseCase {
    List<Vendor> searchActiveVendors(UserRole role, String search);
}
