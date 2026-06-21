package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.application.port.outbound.model.UserInfo;

import java.util.List;
import java.util.Map;

public interface LoadUsersPort {
    Map<String, UserInfo> findByCodes(List<String> userCodes);
}
