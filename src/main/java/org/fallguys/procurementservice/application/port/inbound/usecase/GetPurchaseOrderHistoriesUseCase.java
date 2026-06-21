package org.fallguys.procurementservice.application.port.inbound.usecase;

import org.fallguys.procurementservice.application.port.inbound.model.PurchaseOrderHistoryEntry;
import org.fallguys.procurementservice.domain.model.UserRole;

import java.util.List;

public interface GetPurchaseOrderHistoriesUseCase {
    List<PurchaseOrderHistoryEntry> getHistories(UserRole role, String code);
}
