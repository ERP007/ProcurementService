package org.fallguys.procurementservice.application.port.outbound;

import java.util.Optional;

public interface LoadUserPort {
    Optional<UserInfo> findByCode(String userCode);
}
