package org.fallguys.procurementservice.adapter.outbound.client;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.adapter.outbound.client.dto.UserBatchRequest;
import org.fallguys.procurementservice.adapter.outbound.client.dto.UserBatchResponse;
import org.fallguys.procurementservice.adapter.outbound.client.dto.UserResponse;
import org.fallguys.procurementservice.application.port.outbound.LoadUserPort;
import org.fallguys.procurementservice.application.port.outbound.LoadUsersPort;
import org.fallguys.procurementservice.application.port.outbound.UserInfo;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ExternalServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserClientAdapter implements LoadUserPort, LoadUsersPort {

    private static final String USER_PATH = "/internal/users/{employeeNumber}";
    private static final String BATCH_USER_LIST_PATH = "/internal/users/batch-userList";

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
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getMessage(),
                    e);
        } catch (RestClientException e) {
            throw new ExternalServiceException(
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getMessage(),
                    e);
        }
    }

    /**
     * 사번 목록으로 사용자 정보를 일괄 조회한다.
     *
     * 흐름:
     * 1) POST /internal/users/batch-userList 를 호출한다.
     * 2) 찾은 사용자만 사번을 키로 하는 UserInfo 맵으로 반환한다.
     *    notFoundEmployeeNumbers는 무시 — 조회 enrichment 목적이므로 부분 결과 허용.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖.
     *
     * 예외:
     * - HTTP 오류·연결 실패: ExternalServiceException (PO-07-03, 502)
     */
    @Override
    public Map<String, UserInfo> findByCodes(List<String> userCodes) {
        UserBatchResponse response;
        try {
            response = userRestClient.post()
                    .uri(BATCH_USER_LIST_PATH)
                    .header("Authorization", "Bearer " + ClientTokenExtractor.extractToken())
                    .body(new UserBatchRequest(userCodes))
                    .retrieve()
                    .body(UserBatchResponse.class);
        } catch (RestClientException e) {
            throw new ExternalServiceException(
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getMessage(),
                    e);
        }

        if (response == null || response.users() == null) {
            return Collections.emptyMap();
        }

        return response.users().stream()
                .collect(Collectors.toMap(
                        UserResponse::employeeNumber,
                        u -> new UserInfo(u.employeeNumber(), u.name(), u.position())
                ));
    }
}
