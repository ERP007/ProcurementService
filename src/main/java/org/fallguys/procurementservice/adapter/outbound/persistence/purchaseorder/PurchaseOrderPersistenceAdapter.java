package org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorder;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.adapter.outbound.persistence.purchaseorderline.PurchaseOrderLineEntity;
import org.fallguys.procurementservice.adapter.outbound.persistence.vendor.VendorEntity;
import org.fallguys.procurementservice.adapter.outbound.persistence.vendor.VendorJpaDao;
import org.fallguys.procurementservice.application.port.inbound.query.SearchPurchaseOrderQuery;
import org.fallguys.procurementservice.application.port.inbound.model.SortDirection;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderKpiPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SavePurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.SearchPurchaseOrderPort;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.Money;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderKpi;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderPage;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PurchaseOrderPersistenceAdapter implements SavePurchaseOrderPort, LoadPurchaseOrderPort,
        LoadPurchaseOrderKpiPort, SearchPurchaseOrderPort {

    private final PurchaseOrderJpaDao purchaseOrderJpaDao;
    private final VendorJpaDao vendorJpaDao;

    @Override
    public PurchaseOrder save(PurchaseOrder purchaseOrder) {
        VendorEntity vendor = vendorJpaDao.findById(purchaseOrder.getVendorCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ProcurementErrorCode.VENDOR_NOT_FOUND_ON_DETAIL,
                        ProcurementErrorCode.VENDOR_NOT_FOUND_ON_DETAIL.getMessage() + ": " + purchaseOrder.getVendorCode()
                ));
        PurchaseOrderEntity entity = purchaseOrderJpaDao.findById(purchaseOrder.getCode())
                .map(existing -> existing.update(purchaseOrder, vendor))
                .orElseGet(() -> PurchaseOrderEntity.from(purchaseOrder, vendor));
        return purchaseOrderJpaDao.save(entity).toDomain();
    }

    @Override
    public Optional<PurchaseOrder> findByCode(String code) {
        return purchaseOrderJpaDao.findById(code).map(PurchaseOrderEntity::toDomain);
    }

    @Override
    public PurchaseOrderKpi loadKpi() {
        long totalCount = purchaseOrderJpaDao.countByStatusNot(PurchaseOrderStatus.CANCELED);
        long draftCount = purchaseOrderJpaDao.countByStatus(PurchaseOrderStatus.DRAFT);
        long approvedCount = purchaseOrderJpaDao.countByStatus(PurchaseOrderStatus.APPROVED);
        return new PurchaseOrderKpi(totalCount, draftCount, approvedCount);
    }

    @Override
    public PurchaseOrderPage search(SearchPurchaseOrderQuery query) {
        Sort sort = buildSort(query);
        Page<PurchaseOrderEntity> page = purchaseOrderJpaDao.findAll(
                buildSpec(query),
                PageRequest.of(query.page() - 1, query.size(), sort)
        );

        List<PurchaseOrderSummary> summaries = page.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PurchaseOrderPage(summaries, query.page(), query.size(), page.getTotalElements());
    }

    private Specification<PurchaseOrderEntity> buildSpec(SearchPurchaseOrderQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.search() != null && !query.search().isBlank()) {
                String pattern = "%" + query.search().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("vendor").get("name")), pattern)
                ));
            }

            predicates.add(root.get("status").in(query.statuses()));

            if (query.vendorCode() != null && !query.vendorCode().isBlank()) {
                predicates.add(cb.equal(root.get("vendor").get("code"), query.vendorCode()));
            }

            Instant startInstant = query.startDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant endInstant = query.endDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            predicates.add(cb.greaterThanOrEqualTo(root.get("creation").get("createdAt"), startInstant));
            predicates.add(cb.lessThan(root.get("creation").get("createdAt"), endInstant));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSort(SearchPurchaseOrderQuery query) {
        Sort.Direction direction = query.sortDirection() == SortDirection.ASC
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = switch (query.sortField()) {
            case TOTAL_AMOUNT -> "totalAmount";
            default -> "creation.createdAt";
        };
        return Sort.by(direction, property);
    }

    private PurchaseOrderSummary toSummary(PurchaseOrderEntity entity) {
        List<PurchaseOrderLineEntity> lines = entity.getLines();

        Set<String> units = lines.stream()
                .map(PurchaseOrderLineEntity::getUnitSnapshot)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Integer totalQuantity = null;
        String unit = null;
        if (units.size() == 1) {
            totalQuantity = lines.stream().mapToInt(PurchaseOrderLineEntity::getOrderQuantity).sum();
            unit = units.iterator().next();
        }

        return new PurchaseOrderSummary(
                entity.getCode(),
                entity.getVendor().getCode(),
                entity.getVendor().getName(),
                entity.getCreation().createdAt(),
                lines.size(),
                totalQuantity,
                unit,
                Money.of(entity.getTotalAmount()),
                entity.getStatus()
        );
    }
}
