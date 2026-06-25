package org.fallguys.procurementservice.adapter.outbound.client;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.adapter.outbound.client.dto.InboundStockRequest;
import org.fallguys.procurementservice.application.port.outbound.port.SyncInboundStockPort;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ExternalServiceException;
import org.fallguys.procurementservice.domain.model.purchaseorder.PurchaseOrder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 동기 입고 어댑터: REST로 inventory 입고를 직접 호출(부하 테스트용).
 * 기본(async outbox)은
 * {@link org.fallguys.procurementservice.adapter.outbound.messaging.StockInboundMessagingAdapter} 참고.
 * 사용 여부는 서비스가 {@code stock.sync-mode} 플래그로 분기한다.
 */
@Component
@RequiredArgsConstructor
public class StockInboundRestAdapter implements SyncInboundStockPort {

    private static final String INBOUND_PATH = "/internal/inventory/stocks/inbound";

    private final RestClient inventoryRestClient;

    /**
     * 입고 요청을 inventory에 동기 REST로 전송한다(호출자 트랜잭션 내 수행).
     *
     * 흐름:
     * 1) 도메인 PurchaseOrder → InboundStockRequest(sourceRef·warehouseCode·lines).
     * 2) POST /internal/inventory/stocks/inbound, JWT 전달.
     *
     * 트랜잭션: 호출자 쓰기 트랜잭션 내 동기 호출. 실패 시 예외 전파로 PO 상태·saga 전이 전부 롤백.
     *
     * 예외: 5xx·연결 실패 → ExternalServiceException (PO-07-02, 502).
     */
    @Override
    public void inbound(PurchaseOrder order) {
        try {
            inventoryRestClient.post()
                    .uri(INBOUND_PATH)
                    .header("Authorization", "Bearer " + ClientTokenExtractor.extractToken())
                    .body(InboundStockRequest.from(order))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ExternalServiceException(
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getMessage(),
                    e);
        }
    }
}
