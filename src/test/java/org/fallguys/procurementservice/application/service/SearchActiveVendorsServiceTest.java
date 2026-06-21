package org.fallguys.procurementservice.application.service;

import org.fallguys.procurementservice.application.port.outbound.port.LoadVendorPort;
import org.fallguys.procurementservice.domain.exception.ForbiddenException;
import org.fallguys.procurementservice.domain.model.UserRole;
import org.fallguys.procurementservice.domain.model.vendor.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SearchActiveVendorsServiceTest {

    @Mock
    private LoadVendorPort loadVendorPort;

    @InjectMocks
    private SearchActiveVendorsService service;

    private Vendor vendor1;
    private Vendor vendor2;

    @BeforeEach
    void setUp() {
        vendor1 = new Vendor("SUP-001", "㈜동성정밀", "김담당", "010-1234-5678", "서울시", true);
        vendor2 = new Vendor("SUP-002", "㈜한국부품", "이담당", "010-9876-5432", "부산시", true);
    }

    @Test
    void ADMIN_search_있으면_필터된_공급사_반환() {
        given(loadVendorPort.findAllActiveByNameContaining("동성")).willReturn(List.of(vendor1));

        List<Vendor> result = service.searchActiveVendors(UserRole.ADMIN, "동성");

        assertThat(result).containsExactly(vendor1);
        verify(loadVendorPort).findAllActiveByNameContaining("동성");
    }

    @Test
    void HQ_MANAGER_search_없으면_전체_활성_공급사_반환() {
        given(loadVendorPort.findAllActiveByNameContaining(null)).willReturn(List.of(vendor1, vendor2));

        List<Vendor> result = service.searchActiveVendors(UserRole.HQ_MANAGER, null);

        assertThat(result).containsExactly(vendor1, vendor2);
        verify(loadVendorPort).findAllActiveByNameContaining(null);
    }

    @Test
    void HQ_STAFF_search_있으면_필터된_공급사_반환() {
        given(loadVendorPort.findAllActiveByNameContaining("한국")).willReturn(List.of(vendor2));

        List<Vendor> result = service.searchActiveVendors(UserRole.HQ_STAFF, "한국");

        assertThat(result).containsExactly(vendor2);
        verify(loadVendorPort).findAllActiveByNameContaining("한국");
    }

    @Test
    void BRANCH_MANAGER_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.searchActiveVendors(UserRole.BRANCH_MANAGER, null))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadVendorPort);
    }

    @Test
    void BRANCH_STAFF_역할이면_ForbiddenException_발생() {
        assertThatThrownBy(() -> service.searchActiveVendors(UserRole.BRANCH_STAFF, null))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(loadVendorPort);
    }
}
