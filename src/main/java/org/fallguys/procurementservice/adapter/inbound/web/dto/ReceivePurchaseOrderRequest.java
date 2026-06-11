package org.fallguys.procurementservice.adapter.inbound.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReceivePurchaseOrderRequest(@NotNull LocalDate receivedDate) {}
