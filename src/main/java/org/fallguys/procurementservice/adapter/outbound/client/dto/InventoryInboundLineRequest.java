package org.fallguys.procurementservice.adapter.outbound.client.dto;

public record InventoryInboundLineRequest(
        String sku,
        int quantity,
        Long sourceLineNo
) {}
