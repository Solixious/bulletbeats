package in.bulletbeats.domain.billing.repository;

import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.crm.repository.CustomerRepository;
import in.bulletbeats.domain.shared.enums.BillStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class BillRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    BillRepository billRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    EntityManager entityManager;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Customer saveCustomer(String phone) {
        return customerRepository.save(Customer.builder().name(phone).phone(phone).build());
    }

    private Bill saveBillAt(Customer customer, BillStatus status, LocalDateTime createdAt) {
        Bill bill = billRepository.save(Bill.builder()
                .billNumber("BILL-" + System.nanoTime())
                .customer(customer)
                .status(status)
                .build());
        flushAndClear();
        billRepository.backdateCreatedAt(bill.getId(), createdAt);
        flushAndClear();
        return bill;
    }

    @Test
    void findDistinctPaidCustomerIdsInRange_onlyReturnsPaidBillsWithinRange() {
        LocalDateTime pastWeekStart = LocalDateTime.of(2026, 8, 31, 0, 0);
        LocalDateTime currentWeekStart = LocalDateTime.of(2026, 9, 7, 0, 0);

        Customer inRangePaid = saveCustomer("9000000001");
        saveBillAt(inRangePaid, BillStatus.PAID, pastWeekStart.plusDays(1));

        Customer inRangeDraft = saveCustomer("9000000002");
        saveBillAt(inRangeDraft, BillStatus.DRAFT, pastWeekStart.plusDays(1));

        Customer outOfRangePaid = saveCustomer("9000000003");
        saveBillAt(outOfRangePaid, BillStatus.PAID, currentWeekStart.plusDays(1));

        List<Long> ids = billRepository.findDistinctPaidCustomerIdsInRange(pastWeekStart, currentWeekStart);

        assertThat(ids).containsExactly(inRangePaid.getId());
    }

    @Test
    void findFirstPaidVisitDates_returnsEarliestPaidBillPerCustomer() {
        Customer customer = saveCustomer("9000000004");
        LocalDateTime earliest = LocalDateTime.of(2026, 8, 1, 10, 0);
        LocalDateTime later = LocalDateTime.of(2026, 9, 1, 10, 0);

        saveBillAt(customer, BillStatus.PAID, later);
        saveBillAt(customer, BillStatus.PAID, earliest);
        // A DRAFT bill even earlier must not count as a "visit".
        saveBillAt(customer, BillStatus.DRAFT, earliest.minusDays(10));

        Map<Long, LocalDateTime> firstVisits = billRepository.findFirstPaidVisitDates(List.of(customer.getId())).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (LocalDateTime) row[1]));

        assertThat(firstVisits.get(customer.getId())).isEqualTo(earliest);
    }

    @Test
    void retentionClassification_matchesExpectedNewAndReturningCohorts() {
        LocalDateTime pastWeekStart = LocalDateTime.of(2026, 8, 31, 0, 0);
        LocalDateTime currentWeekStart = LocalDateTime.of(2026, 9, 7, 0, 0);
        LocalDateTime currentWeekEnd = LocalDateTime.of(2026, 9, 14, 0, 0);

        // New last week, returns this week -> retained new customer.
        Customer newRetained = saveCustomer("9000000010");
        saveBillAt(newRetained, BillStatus.PAID, pastWeekStart.plusDays(1));
        saveBillAt(newRetained, BillStatus.PAID, currentWeekStart.plusDays(1));

        // New last week, does not return -> new but not retained.
        Customer newChurned = saveCustomer("9000000011");
        saveBillAt(newChurned, BillStatus.PAID, pastWeekStart.plusDays(2));

        // Old customer (first visit long ago), visits last week and this week -> retained returning customer.
        Customer oldRetained = saveCustomer("9000000012");
        saveBillAt(oldRetained, BillStatus.PAID, pastWeekStart.minusWeeks(4));
        saveBillAt(oldRetained, BillStatus.PAID, pastWeekStart.plusDays(3));
        saveBillAt(oldRetained, BillStatus.PAID, currentWeekStart.plusDays(2));

        // Old customer, visits last week but not this week -> old but not retained.
        Customer oldChurned = saveCustomer("9000000013");
        saveBillAt(oldChurned, BillStatus.PAID, pastWeekStart.minusWeeks(2));
        saveBillAt(oldChurned, BillStatus.PAID, pastWeekStart.plusDays(4));

        List<Long> pastWeekVisitorIds =
                billRepository.findDistinctPaidCustomerIdsInRange(pastWeekStart, currentWeekStart);
        List<Long> currentWeekVisitorIds =
                billRepository.findDistinctPaidCustomerIdsInRange(currentWeekStart, currentWeekEnd);
        Map<Long, LocalDateTime> firstVisitByCustomer = billRepository.findFirstPaidVisitDates(pastWeekVisitorIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (LocalDateTime) row[1]));

        assertThat(pastWeekVisitorIds).containsExactlyInAnyOrder(
                newRetained.getId(), newChurned.getId(), oldRetained.getId(), oldChurned.getId());
        assertThat(currentWeekVisitorIds).containsExactlyInAnyOrder(newRetained.getId(), oldRetained.getId());

        assertThat(firstVisitByCustomer.get(newRetained.getId())).isAfterOrEqualTo(pastWeekStart);
        assertThat(firstVisitByCustomer.get(newChurned.getId())).isAfterOrEqualTo(pastWeekStart);
        assertThat(firstVisitByCustomer.get(oldRetained.getId())).isBefore(pastWeekStart);
        assertThat(firstVisitByCustomer.get(oldChurned.getId())).isBefore(pastWeekStart);
    }
}
