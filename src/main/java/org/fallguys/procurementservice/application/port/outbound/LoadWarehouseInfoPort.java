package org.fallguys.procurementservice.application.port.outbound;

public interface LoadWarehouseInfoPort {
    WarehouseInfo findByCode(String warehouseCode);
}
