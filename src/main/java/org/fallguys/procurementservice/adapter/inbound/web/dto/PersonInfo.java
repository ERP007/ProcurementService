package org.fallguys.procurementservice.adapter.inbound.web.dto;

import org.fallguys.procurementservice.application.port.outbound.model.UserInfo;

public record PersonInfo(String code, String name, String position) {

    public static PersonInfo from(UserInfo userInfo) {
        if (userInfo == null) return null;
        return new PersonInfo(userInfo.code(), userInfo.name(), userInfo.position());
    }
}
