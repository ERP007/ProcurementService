package org.fallguys.procurementservice.application.port.outbound;

import org.fallguys.procurementservice.domain.model.PurchaseOrder;

import java.util.Optional;

public interface LoadPurchaseOrderPort {
    Optional<PurchaseOrder> findByCode(String code);
}
