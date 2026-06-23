package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.application.port.outbound.model.WarehouseInfo;

public interface LoadWarehouseInfoPort {
    WarehouseInfo findByCode(String warehouseCode);
}
