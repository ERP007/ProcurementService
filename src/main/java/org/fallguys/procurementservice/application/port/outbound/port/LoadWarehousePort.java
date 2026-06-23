package org.fallguys.procurementservice.application.port.outbound.port;

public interface LoadWarehousePort {
    void verifyActive(String warehouseCode);
}
