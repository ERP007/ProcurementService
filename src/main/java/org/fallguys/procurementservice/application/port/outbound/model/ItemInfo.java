package org.fallguys.procurementservice.application.port.outbound.model;

public record ItemInfo(
        String itemSku,
        String itemName,
        String unit
) {}
