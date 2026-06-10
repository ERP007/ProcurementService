package org.fallguys.procurementservice.adapter.outbound.persistence;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.outbound.LoadPurchaseOrderKpiPort;
import org.fallguys.procurementservice.application.port.outbound.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.SavePurchaseOrderPort;
import org.fallguys.procurementservice.domain.model.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.PurchaseOrderKpi;
import org.fallguys.procurementservice.domain.model.PurchaseOrderStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PurchaseOrderPersistenceAdapter implements SavePurchaseOrderPort, LoadPurchaseOrderPort, LoadPurchaseOrderKpiPort {

    private final PurchaseOrderJpaDao purchaseOrderJpaDao;

    @Override
    public PurchaseOrder save(PurchaseOrder purchaseOrder) {
        PurchaseOrderEntity entity = purchaseOrderJpaDao.findById(purchaseOrder.getCode())
                .map(existing -> existing.update(purchaseOrder))
                .orElseGet(() -> PurchaseOrderEntity.from(purchaseOrder));
        return purchaseOrderJpaDao.save(entity).toDomain();
    }

    @Override
    public Optional<PurchaseOrder> findByCode(String code) {
        return purchaseOrderJpaDao.findById(code).map(PurchaseOrderEntity::toDomain);
    }

    @Override
    public PurchaseOrderKpi loadKpi(LocalDate today) {
        long totalCount = purchaseOrderJpaDao.countByStatusNot(PurchaseOrderStatus.CANCELED);
        long draftCount = purchaseOrderJpaDao.countByStatus(PurchaseOrderStatus.DRAFT);
        long approvedCount = purchaseOrderJpaDao.countByStatus(PurchaseOrderStatus.APPROVED);
        long delayedCount = purchaseOrderJpaDao.countByStatusAndDesiredArrivalDateBefore(PurchaseOrderStatus.APPROVED, today);
        return new PurchaseOrderKpi(totalCount, draftCount, approvedCount, delayedCount);
    }
}
