package org.fallguys.procurementservice.application.port.outbound;

public interface LoadWarehousePort {
    void verifyActive(String warehouseCode);
}
