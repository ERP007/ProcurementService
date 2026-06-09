package org.fallguys.procurementservice.application.service;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.inbound.SearchActiveVendorsUseCase;
import org.fallguys.procurementservice.application.port.outbound.LoadVendorPort;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.fallguys.procurementservice.domain.model.Vendor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchActiveVendorsService implements SearchActiveVendorsUseCase {

    private static final Set<UserRole> ALLOWED_ROLES = EnumSet.of(
            UserRole.ADMIN,
            UserRole.HQ_MANAGER,
            UserRole.HQ_STAFF
    );

    private final LoadVendorPort loadVendorPort;

    /**
     * 활성 공급사 목록을 조회한다.
     *
     * 흐름:
     * 1) 호출자 역할이 허용 역할(ADMIN, HQ_MANAGER, HQ_STAFF)인지 검증한다.
     * 2) search가 있으면 공급사명 부분 일치 필터 적용, 없으면 전체 활성 공급사 반환.
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - 허용되지 않은 역할: ForbiddenException (403 매핑)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Vendor> searchActiveVendors(UserRole role, String search) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ForbiddenException(ProcurementErrorCode.FORBIDDEN);
        }
        return loadVendorPort.findAllActiveByNameContaining(search);
    }
}
