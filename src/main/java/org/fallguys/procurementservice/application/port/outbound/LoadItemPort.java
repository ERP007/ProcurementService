package org.fallguys.procurementservice.application.port.outbound;

import java.util.List;
import java.util.Map;

public interface LoadItemPort {
    Map<String, ItemInfo> loadAll(List<String> itemCodes);
}
