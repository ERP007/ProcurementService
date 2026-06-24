package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.application.port.outbound.model.UserInfo;
import org.fallguys.procurementservice.domain.model.purchaseorderhistory.ActorRef;

public record PersonInfo(String code, String name, String position) {

    public static PersonInfo from(UserInfo userInfo) {
        if (userInfo == null) return null;
        return new PersonInfo(userInfo.code(), userInfo.name(), userInfo.position());
    }

    // 박제된 행위자 스냅샷에서 직접 구성(외부 호출 없음).
    public static PersonInfo from(ActorRef actor) {
        if (actor == null) return null;
        return new PersonInfo(actor.code(), actor.name(), actor.position());
    }
}
