package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.domain.model.vendor.Vendor;

public record VendorResponse(
        String code,
        String name,
        boolean active
) {
    public static VendorResponse from(Vendor vendor) {
        return new VendorResponse(vendor.getCode(), vendor.getName(), vendor.isActive());
    }
}
