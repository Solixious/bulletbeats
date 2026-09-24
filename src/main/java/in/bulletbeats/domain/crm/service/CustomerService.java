package in.bulletbeats.domain.crm.service;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.crm.entity.CustomerNote;
import in.bulletbeats.domain.crm.repository.CustomerNoteRepository;
import in.bulletbeats.domain.crm.repository.CustomerRepository;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerNoteRepository customerNoteRepository;

    private static final ZoneId CAFE_ZONE = ZoneId.of("Asia/Kolkata");

    @Transactional
    public Customer findOrCreateByPhone(String phone, String name, Long createdByUserId) {
        return customerRepository.findByPhone(phone).orElseGet(() -> {
            Customer newCustomer = Customer.builder()
                    .phone(phone)
                    .name(name)
                    .promoBucket(customerRepository.findLeastPopulatedPromoBucket())
                    .build();
            return customerRepository.save(newCustomer);
        });
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    /** Today's promo bucket: Monday = 1 ... Sunday = 7, in café local time. */
    public int todaysPromoBucket() {
        return LocalDate.now(CAFE_ZONE).getDayOfWeek().getValue();
    }

    /** Customers in the bucket with at least one PAID bill, i.e. who receive that day's promo. */
    public List<Customer> getPromoAudience(int bucket) {
        return customerRepository.findPromoAudience(bucket, BillStatus.PAID);
    }

    /** Promo audience size for each bucket 1..7 (zero-filled). */
    public Map<Integer, Long> getPromoAudienceCounts() {
        Map<Integer, Long> counts = new TreeMap<>();
        for (int b = 1; b <= 7; b++) counts.put(b, 0L);
        for (Object[] row : customerRepository.countPromoAudienceByBucket(BillStatus.PAID)) {
            counts.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }

    public List<Customer> search(String query) {
        if (query == null || query.isBlank()) {
            return customerRepository.findAllByOrderByIsVipDescNameAsc();
        }
        return customerRepository.search(query);
    }

    @Transactional
    public void toggleVip(Long id, Long updatedByUserId) {
        Customer customer = getById(id);
        customer.setVip(!customer.isVip());
        customerRepository.save(customer);
    }

    @Transactional
    public void addNote(Long customerId, String noteText, Long createdByUserId) {
        Customer customer = getById(customerId);
        CustomerNote note = CustomerNote.builder()
                .customer(customer)
                .note(noteText)
                .createdBy(createdByUserId)
                .build();
        customerNoteRepository.save(note);
        customer.setNotesCount(customer.getNotesCount() + 1);
        customerRepository.save(customer);
    }

    public boolean existsByPhone(String phone) {
        return customerRepository.existsByPhone(phone);
    }

    public List<CustomerNote> getNotesForCustomer(Long customerId) {
        return customerNoteRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public void recordVisit(Long customerId, BigDecimal billTotal) {
        Customer customer = getById(customerId);
        customer.setVisitCount(customer.getVisitCount() + 1);
        customer.setTotalSpend(customer.getTotalSpend().add(billTotal));
        customer.setLastVisitDate(LocalDateTime.now());
        customerRepository.save(customer);
    }

    @Transactional
    public void markAsStudent(Long customerId, Long updatedByUserId) {
        Customer customer = getById(customerId);
        if (customer.getName() == null || customer.getName().isBlank()) {
            throw new IllegalStateException(
                    "Cannot mark phone-only customer as student. Please update customer name first.");
        }
        customer.setStudent(true);
        customerRepository.save(customer);
    }

    @Transactional
    public void unmarkAsStudent(Long customerId, Long updatedByUserId) {
        Customer customer = getById(customerId);
        customer.setStudent(false);
        customerRepository.save(customer);
    }

    @Transactional
    public Customer updateContact(Long id, String name, String phone, Long updatedByUserId) {
        Customer customer = getById(id);
        String trimmedPhone = phone.trim();
        if (!trimmedPhone.equals(customer.getPhone()) && customerRepository.existsByPhone(trimmedPhone)) {
            throw new IllegalArgumentException("Phone " + trimmedPhone + " is already registered to another customer");
        }
        customer.setName(name.trim());
        customer.setPhone(trimmedPhone);
        return customerRepository.save(customer);
    }

    @Transactional
    public void incrementStudentDiscountCount(Long customerId) {
        Customer customer = getById(customerId);
        customer.setStudentDiscountCount(customer.getStudentDiscountCount() + 1);
        customerRepository.save(customer);
    }
}
