package org.fallguys.procurementservice.application.port.outbound.port;

import org.fallguys.procurementservice.application.port.outbound.model.UserActivity;

public interface PublishUserActivityPort {

    // 사용자 활동을 user 서비스로 비동기 발행한다(MQ 직접, 유실 가능). 실패해도 호출자 흐름은 막지 않는다.
    void publish(UserActivity activity);
}
