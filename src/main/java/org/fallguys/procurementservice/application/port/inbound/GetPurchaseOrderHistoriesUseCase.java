package org.fallguys.procurementservice.application.port.inbound;

import org.fallguys.procurementservice.domain.model.UserRole;

import java.util.List;

public interface GetPurchaseOrderHistoriesUseCase {
    List<PurchaseOrderHistoryEntry> getHistories(UserRole role, String code);
}
