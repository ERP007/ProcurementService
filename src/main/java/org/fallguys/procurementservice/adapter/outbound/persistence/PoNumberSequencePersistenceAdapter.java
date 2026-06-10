package org.fallguys.procurementservice.adapter.outbound.persistence;

import lombok.RequiredArgsConstructor;
import org.fallguys.procurementservice.application.port.outbound.GeneratePoCodePort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class PoNumberSequencePersistenceAdapter implements GeneratePoCodePort {

    private final JdbcTemplate jdbcTemplate;

    /**
     * PO 코드를 채번한다.
     *
     * 흐름:
     * 1) 당월 첫날 키로 upsert를 실행한다.
     *    - 행 없음: last_seq = 1 로 INSERT
     *    - 행 있음: last_seq = last_seq + 1 로 UPDATE
     * 2) RETURNING last_seq 로 채번된 값을 단일 쿼리에서 반환한다.
     * 3) PO-YYYY-MM-NNNN 형식으로 코드를 조합한다.
     *
     * 트랜잭션: 호출 측(서비스)의 쓰기 트랜잭션에 참여한다.
     * PostgreSQL이 행 수준 원자성을 보장하므로 별도 락 불필요.
     */
    @Override
    public String generate() {
        LocalDate today = LocalDate.now();
        LocalDate monthKey = today.withDayOfMonth(1);

        Integer lastSeq = jdbcTemplate.queryForObject("""
                INSERT INTO po_number_sequences (seq_date, last_seq)
                VALUES (?, 1)
                ON CONFLICT (seq_date)
                DO UPDATE SET last_seq = po_number_sequences.last_seq + 1
                RETURNING last_seq
                """,
                Integer.class,
                monthKey);

        return String.format("PO-%d-%02d-%04d", today.getYear(), today.getMonthValue(), lastSeq);
    }
}
