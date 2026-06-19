package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.usecase.GetPurchaseOrderHistoriesUseCase;
import org.fallguys.procurementservice.application.port.inbound.model.PurchaseOrderHistoryEntry;
import org.fallguys.procurementservice.application.port.outbound.port.LoadPurchaseOrderPort;
import org.fallguys.procurementservice.application.port.outbound.port.LoadUsersPort;
import org.fallguys.procurementservice.application.port.outbound.model.UserInfo;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrderStatus;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class GetPurchaseOrderHistoriesService implements GetPurchaseOrderHistoriesUseCase {

    private static final Set<UserRole> ALLOWED_ROLES = EnumSet.of(
            UserRole.ADMIN,
            UserRole.HQ_MANAGER,
            UserRole.HQ_STAFF
    );

    private final LoadPurchaseOrderPort loadPurchaseOrderPort;
    private final LoadUsersPort loadUsersPort;

    /**
     * 발주서의 상태 변경 이력을 최신순으로 조회한다.
     *
     * 흐름:
     * 1) 역할 검증: ADMIN·HQ_MANAGER·HQ_STAFF만 허용.
     * 2) code로 발주서를 조회한다(없으면 404).
     * 3) 이력 관련 유저코드를 수집하여 User 서비스 batch 호출로 담당자 정보를 조회한다.
     * 4) creation·approval·receiving·cancellation에서 이력 항목을 조립한다.
     * 5) changedAt 기준 내림차순 정렬 후 반환한다.
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403)
     * - 발주서 미존재: ResourceNotFoundException (PO-02-04, 404)
     * - 유저 조회 실패: ExternalServiceException (PO-07-03, 502)
     */
    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderHistoryEntry> getHistories(UserRole role, String code) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ForbiddenException(CommonErrorCode.FORBIDDEN);
        }

        PurchaseOrder order = loadPurchaseOrderPort.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(ProcurementErrorCode.PURCHASE_ORDER_NOT_FOUND));

        List<String> actorCodes = collectActorCodes(order);
        Map<String, UserInfo> userInfoMap = actorCodes.isEmpty()
                ? Map.of()
                : loadUsersPort.findByCodes(actorCodes);

        return buildEntries(order, userInfoMap);
    }

    private List<String> collectActorCodes(PurchaseOrder order) {
        Stream.Builder<String> builder = Stream.builder();
        builder.accept(order.getCreation().createdBy());
        if (order.getApproval() != null) builder.accept(order.getApproval().approvedBy());
        if (order.getReceiving() != null) builder.accept(order.getReceiving().receivedBy());
        if (order.getCancellation() != null) builder.accept(order.getCancellation().canceledBy());
        return builder.build().filter(Objects::nonNull).distinct().toList();
    }

    private List<PurchaseOrderHistoryEntry> buildEntries(PurchaseOrder order, Map<String, UserInfo> userInfoMap) {
        List<PurchaseOrderHistoryEntry> entries = new ArrayList<>();

        entries.add(new PurchaseOrderHistoryEntry(
                PurchaseOrderStatus.DRAFT,
                userInfoMap.get(order.getCreation().createdBy()),
                order.getCreation().createdAt()
        ));
        if (order.getApproval() != null) {
            entries.add(new PurchaseOrderHistoryEntry(
                    PurchaseOrderStatus.APPROVED,
                    userInfoMap.get(order.getApproval().approvedBy()),
                    order.getApproval().approvedAt()
            ));
        }
        if (order.getReceiving() != null) {
            entries.add(new PurchaseOrderHistoryEntry(
                    PurchaseOrderStatus.RECEIVED,
                    userInfoMap.get(order.getReceiving().receivedBy()),
                    order.getReceiving().receivedAt()
            ));
        }
        if (order.getCancellation() != null) {
            entries.add(new PurchaseOrderHistoryEntry(
                    PurchaseOrderStatus.CANCELED,
                    userInfoMap.get(order.getCancellation().canceledBy()),
                    order.getCancellation().canceledAt()
            ));
        }

        entries.sort(Comparator.comparing(PurchaseOrderHistoryEntry::changedAt).reversed());
        return entries;
    }
}
