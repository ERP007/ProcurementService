package org.fallguys.procurementservice.adapter.outbound.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fallguys.procurementservice.adapter.outbound.client.dto.InventoryInboundLineRequest;
import org.fallguys.procurementservice.adapter.outbound.client.dto.InventoryInboundRequest;
import org.fallguys.procurementservice.application.port.outbound.InboundStockPort;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ExternalServiceException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.model.PurchaseOrder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClientAdapter implements InboundStockPort {

    private static final String INBOUND_PATH = "/internal/inventory/stocks/inbound";

    private final RestClient inventoryRestClient;

    /**
     * 발주 입고 시 재고 서비스에 입고 이력을 기록한다.
     *
     * 흐름:
     * 1) PurchaseOrder에서 code·warehouseCode·라인(itemSku·orderQuantity·id)을 추출해 요청을 구성한다.
     * 2) POST /internal/inventory/stocks/inbound 를 호출한다.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖. 실패 시 호출자(서비스)가 롤백.
     *
     * 예외:
     * - 4xx: BusinessValidationException (PO-07-04, 400)
     * - 5xx·연결 실패: ExternalServiceException (PO-07-02, 502)
     */
    @Override
    public void inbound(PurchaseOrder order) {
        InventoryInboundRequest request = buildInboundRequest(order);
        try {
            inventoryRestClient.post()
                    .uri(INBOUND_PATH)
                    .header("Authorization", "Bearer " + ClientTokenExtractor.extractToken())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                throw new BusinessValidationException(ProcurementErrorCode.INVENTORY_INBOUND_FAILED);
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

    private InventoryInboundRequest buildInboundRequest(PurchaseOrder order) {
        List<InventoryInboundLineRequest> lines = order.getLines().stream()
                .map(line -> new InventoryInboundLineRequest(
                        line.getItemSku(),
                        line.getOrderQuantity(),
                        line.getId()))
                .toList();
        return new InventoryInboundRequest(order.getCode(), order.getWarehouseCode(), lines);
    }
}
