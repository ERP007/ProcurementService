package org.fallguys.procurementservice.adapter.inbound.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * inventory reply payload. 처리 결과(status)와 실패 사유만 매핑하고
 * movements 등 미사용 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryReplyPayload(String status, String errorCode, String errorMessage) {}
