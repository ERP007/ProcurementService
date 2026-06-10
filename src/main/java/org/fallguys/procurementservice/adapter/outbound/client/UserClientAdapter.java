package org.fallguys.procurementservice.adapter.outbound.client;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.adapter.outbound.client.dto.UserResponse;
import org.fallguys.procurementservice.application.port.outbound.LoadUserPort;
import org.fallguys.procurementservice.application.port.outbound.UserInfo;
import org.fallguys.procurementservice.domain.exception.ExternalServiceException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserClientAdapter implements LoadUserPort {

    private static final String USER_PATH = "/internal/users/{employeeNumber}";

    private final RestClient userRestClient;

    /**
     * 사번으로 유저 정보를 조회한다.
     *
     * 흐름:
     * 1) GET /internal/users/{employeeNumber}를 호출한다.
     * 2) 404이면 Optional.empty()를 반환한다.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖.
     *
     * 예외:
     * - 유저 미존재 (404): Optional.empty() 반환
     * - 5xx·연결 실패: ExternalServiceException (PO-07-03, 502)
     */
    @Override
    public Optional<UserInfo> findByCode(String userCode) {
        try {
            UserResponse response = userRestClient.get()
                    .uri(USER_PATH, userCode)
                    .header("Authorization", "Bearer " + ClientTokenExtractor.extractToken())
                    .retrieve()
                    .body(UserResponse.class);
            if (response == null) {
                return Optional.empty();
            }
            return Optional.of(new UserInfo(response.employeeNumber(), response.name(), response.position()));
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                return Optional.empty();
            }
            throw new ExternalServiceException(
                    ProcurementErrorCode.USER_SERVICE_ERROR.getCode(),
                    ProcurementErrorCode.USER_SERVICE_ERROR.getMessage(),
                    e);
        } catch (RestClientException e) {
            throw new ExternalServiceException(
                    ProcurementErrorCode.USER_SERVICE_ERROR.getCode(),
                    ProcurementErrorCode.USER_SERVICE_ERROR.getMessage(),
                    e);
        }
    }
}
