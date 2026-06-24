package org.fallguys.procurementservice.application.port.inbound.usecase;

import org.fallguys.procurementservice.application.port.inbound.model.PurchaseOrderProgressView;
import org.fallguys.procurementservice.domain.model.UserRole;

public interface GetPurchaseOrderProgressUseCase {
    PurchaseOrderProgressView get(UserRole role, String code);
}
