package org.fallguys.procurementservice.application.port.outbound;

public interface LoadWarehousePort {
    boolean existsByCode(String warehouseCode);
}
