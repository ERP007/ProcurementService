package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;

import java.util.Optional;

public interface LoadPurchaseOrderPort {
    Optional<PurchaseOrder> findByCode(String code);
}
