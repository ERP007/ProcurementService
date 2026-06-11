package org.fallguys.procurementservice.application.port.outbound;

import java.util.List;
import java.util.Map;

public interface LoadUsersPort {
    Map<String, UserInfo> findByCodes(List<String> userCodes);
}
