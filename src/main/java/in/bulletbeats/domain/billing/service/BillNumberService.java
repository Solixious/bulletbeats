package in.bulletbeats.domain.billing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class BillNumberService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public String generateBillNumber() {
        LocalDate today = LocalDate.now();
        Integer seq = jdbcTemplate.queryForObject(
                "INSERT INTO bill_sequence(bill_date, last_seq) VALUES(?, 1) " +
                "ON CONFLICT (bill_date) DO UPDATE SET last_seq = bill_sequence.last_seq + 1 " +
                "RETURNING last_seq",
                Integer.class, today);
        return "BB-" + today.format(DATE_FMT) + "-" + String.format("%04d", seq);
    }
}
