package org.fallguys.procurementservice.adapter.outbound.client.dto;

import java.util.List;

public record UserBatchResponse(
        List<UserResponse> users,
        List<String> notFoundEmployeeNumbers
) {}
