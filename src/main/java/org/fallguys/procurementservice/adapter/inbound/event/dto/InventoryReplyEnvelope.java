package org.fallguys.procurementservice.adapter.inbound.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * inventory reply의 BaseEvent envelope. 필요한 correlationId·payload만 매핑하고
 * eventId/eventType 등 나머지 envelope 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryReplyEnvelope(String correlationId, InventoryReplyPayload payload) {}
