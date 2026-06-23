package org.fallguys.procurementservice.application.port.outbound.model;

// 입고 실행자. name은 nullable(JWT name claim 부재 가능).
public record Executor(String code, String name) {}
