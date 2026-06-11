package org.fallguys.procurementservice.application.port.outbound;

public record ItemInfo(
        String itemSku,
        String itemName,
        String unit
) {}
