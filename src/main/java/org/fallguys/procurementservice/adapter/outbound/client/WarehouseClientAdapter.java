package org.fallguys.procurementservice.adapter.outbound.client;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.adapter.outbound.client.dto.WarehouseResponse;
import org.fallguys.procurementservice.application.port.outbound.LoadWarehousePort;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.ExternalServiceException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class WarehouseClientAdapter implements LoadWarehousePort {

    private static final String WAREHOUSE_PATH = "/internal/inventory/warehouses/{code}";

    private final RestClient inventoryRestClient;

    /**
     * 창고 코드로 활성 여부를 확인한다.
     *
     * 흐름:
     * 1) GET /internal/inventory/warehouses/{code} 를 호출한다.
     * 2) 응답의 active 필드가 false면 BusinessValidationException을 던진다.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖. 실패 시 호출자(서비스)가 롤백.
     *
     * 예외:
     * - 창고 미존재 (404): ResourceNotFoundException (PO-02-02, 404)
     * - active=false: BusinessValidationException (PO-03-04, 400)
     * - 5xx·연결 실패: ExternalServiceException (PO-07-02, 502)
     */
    @Override
    public void verifyActive(String warehouseCode) {
        WarehouseResponse response = fetchWarehouse(warehouseCode);
        if (!response.active()) {
            throw new BusinessValidationException(ProcurementErrorCode.WAREHOUSE_INACTIVE);
        }
    }

    private WarehouseResponse fetchWarehouse(String warehouseCode) {
        try {
            WarehouseResponse response = inventoryRestClient.get()
                    .uri(WAREHOUSE_PATH, warehouseCode)
                    .header("Authorization", "Bearer " + ClientTokenExtractor.extractToken())
                    .retrieve()
                    .body(WarehouseResponse.class);
            if (response == null) {
                throw new ExternalServiceException(
                        ProcurementErrorCode.INVENTORY_SERVICE_ERROR.getCode(),
                        ProcurementErrorCode.INVENTORY_SERVICE_ERROR.getMessage(),
                        null);
            }
            return response;
        } catch (HttpStatusCodeException e) {
            if (HttpStatus.resolve(e.getStatusCode().value()) == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException(ProcurementErrorCode.WAREHOUSE_NOT_FOUND);
            }
            throw new ExternalServiceException(
                    ProcurementErrorCode.INVENTORY_SERVICE_ERROR.getCode(),
                    ProcurementErrorCode.INVENTORY_SERVICE_ERROR.getMessage(),
                    e);
        } catch (RestClientException e) {
            throw new ExternalServiceException(
                    ProcurementErrorCode.INVENTORY_SERVICE_ERROR.getCode(),
                    ProcurementErrorCode.INVENTORY_SERVICE_ERROR.getMessage(),
                    e);
        }
    }
}
