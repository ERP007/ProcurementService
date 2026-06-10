package org.fallguys.procurementservice.adapter.outbound.client.dto;

import java.util.List;

public record ItemBatchRequest(
        List<String> skus
) {}
