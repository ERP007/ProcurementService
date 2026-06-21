package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.application.port.outbound.model.UserInfo;

import java.util.Optional;

public interface LoadUserPort {
    Optional<UserInfo> findByCode(String userCode);
}
