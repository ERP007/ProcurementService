package org.fallguys.procurementservice.domain.model.purchaseorder;

/**
 * 발주서가 참조하는 공급사. code(불변 식별자) + nameSnapshot(확정 시점 박제값) 쌍.
 *
 * code         : 항상 보관하는 참조 식별자.
 * nameSnapshot : 확정(승인) 시점에 박제한 공급사명. DRAFT는 아직 미확정이라 null이며,
 *                조회 시 현재 master를 live로 채운다(read 전용 enrich, 저장 X).
 */
public record VendorRef(String code, String nameSnapshot) {

    // DRAFT 등 미확정 참조. 표시명은 박제하지 않고 code만 보관한다.
    public static VendorRef codeOnly(String code) {
        return new VendorRef(code, null);
    }

    // 확정 시점 박제. 신뢰 가능한 출처(공급사 master)에서 조회한 name을 고정한다.
    public static VendorRef snapshot(String code, String name) {
        return new VendorRef(code, name);
    }
}
