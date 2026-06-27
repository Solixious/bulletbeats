package in.bulletbeats.domain.crm.service;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.crm.entity.LoyaltyTransaction;
import in.bulletbeats.domain.crm.repository.LoyaltyTransactionRepository;
import in.bulletbeats.domain.shared.enums.LoyaltyTransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoyaltyService {

    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final CustomerService customerService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void earnPoints(Long customerId, BigDecimal billTotal,
                           Long billId, Long createdByUserId) {
        BigDecimal earnRate = readEarnRate();
        int points = billTotal.divide(earnRate, 0, RoundingMode.FLOOR).intValue();
        if (points == 0) {
            return;
        }

        Customer customer = customerService.getById(customerId);
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customer(customer)
                .points(points)
                .transactionType(LoyaltyTransactionType.EARN)
                .billId(billId)
                .description("Earned from Bill #" + billId)
                .createdBy(createdByUserId)
                .build();

        loyaltyTransactionRepository.save(transaction);
    }

    public Page<LoyaltyTransaction> getTransactionsForCustomer(Long customerId, Pageable pageable) {
        return loyaltyTransactionRepository.findByCustomerId(customerId, pageable);
    }

    public int getPointsBalance(Long customerId) {
        return customerService.getById(customerId).getLoyaltyPoints();
    }

    private BigDecimal readEarnRate() {
        try {
            String value = jdbcTemplate.queryForObject(
                    "SELECT value FROM app_config WHERE key = 'loyalty.earn_rate'",
                    String.class);
            return new BigDecimal(value);
        } catch (EmptyResultDataAccessException e) {
            return new BigDecimal("10.00");
        }
    }
}
