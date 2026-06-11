package org.fallguys.procurementservice.adapter.outbound.client;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.adapter.outbound.client.dto.ItemBatchRequest;
import org.fallguys.procurementservice.adapter.outbound.client.dto.ItemBatchResponse;
import org.fallguys.procurementservice.application.port.outbound.ItemInfo;
import org.fallguys.procurementservice.application.port.outbound.LoadItemPort;
import org.fallguys.procurementservice.domain.exception.BusinessValidationException;
import org.fallguys.procurementservice.domain.exception.CommonErrorCode;
import org.fallguys.procurementservice.domain.exception.ExternalServiceException;
import org.fallguys.procurementservice.domain.exception.ProcurementErrorCode;
import org.fallguys.procurementservice.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ItemClientAdapter implements LoadItemPort {

    private static final String ITEMS_PATH = "/internal/items/batch";

    private final RestClient itemRestClient;

    /**
     * SKU 목록으로 품목 정보를 일괄 조회한다.
     *
     * 흐름:
     * 1) SKU 목록을 body에 담아 POST /internal/items/batch 호출한다.
     * 2) notFoundSkus가 존재하면 ResourceNotFoundException을 던진다.
     * 3) active=false 품목이 존재하면 BusinessValidationException을 던진다.
     * 4) SKU를 키로 하는 ItemInfo 맵을 반환한다.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖. 실패 시 호출자(서비스)가 롤백.
     *
     * 예외:
     * - 미존재 SKU: ResourceNotFoundException (PO-02-03, 404)
     * - 비활성 품목: BusinessValidationException (PO-03-03, 400)
     * - HTTP 오류·연결 실패: ExternalServiceException (PO-07-01, 502)
     */
    @Override
    public Map<String, ItemInfo> loadAll(List<String> itemSkus) {
        ItemBatchRequest request = new ItemBatchRequest(itemSkus);

        ItemBatchResponse response;
        try {
            response = itemRestClient.post()
                    .uri(ITEMS_PATH)
                    .body(request)
                    .retrieve()
                    .body(ItemBatchResponse.class);
        } catch (RestClientException e) {
            throw new ExternalServiceException(
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getMessage(),
                    e);
        }

        if (response == null) {
            throw new ExternalServiceException(
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getMessage(),
                    null);
        }

        if (response.notFoundSkus() != null && !response.notFoundSkus().isEmpty()) {
            throw new ResourceNotFoundException(ProcurementErrorCode.ITEM_NOT_FOUND);
        }

        List<String> inactiveSkus = response.items().stream()
                .filter(item -> !item.active())
                .map(ItemBatchResponse.ItemData::sku)
                .toList();
        if (!inactiveSkus.isEmpty()) {
            throw new BusinessValidationException(ProcurementErrorCode.ITEM_INACTIVE);
        }

        return response.items().stream()
                .collect(Collectors.toMap(
                        ItemBatchResponse.ItemData::sku,
                        item -> new ItemInfo(item.sku(), item.name(), item.unit())
                ));
    }
}
