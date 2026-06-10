package org.fallguys.procurementservice.adapter.outbound.persistence;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.outbound.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.SavePurchaseOrderPort;
import org.fallguys.procurementservice.domain.model.PurchaseOrder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PurchaseOrderPersistenceAdapter implements SavePurchaseOrderPort, LoadPurchaseOrderPort {

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
}
