package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.application.port.outbound.model.ItemInfo;

import java.util.List;
import java.util.Map;

public interface LoadItemPort {
    Map<String, ItemInfo> loadAll(List<String> itemCodes);
}
