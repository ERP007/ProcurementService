package org.fallguys.procurementservice.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount) {

    public Money {
        Objects.requireNonNull(amount, "금액은 null일 수 없습니다");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다");
        }
    }

    public Money multiply(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("수량은 0 이상이어야 합니다");
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)));
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }
}
