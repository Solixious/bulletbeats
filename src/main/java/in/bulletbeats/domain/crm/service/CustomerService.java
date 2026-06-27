package in.bulletbeats.domain.crm.service;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.crm.entity.CustomerNote;
import in.bulletbeats.domain.crm.repository.CustomerNoteRepository;
import in.bulletbeats.domain.crm.repository.CustomerRepository;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerNoteRepository customerNoteRepository;

    @Transactional
    public Customer findOrCreateByPhone(String phone, String name, Long createdByUserId) {
        return customerRepository.findByPhone(phone).orElseGet(() -> {
            Customer newCustomer = Customer.builder()
                    .phone(phone)
                    .name(name)
                    .build();
            return customerRepository.save(newCustomer);
        });
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
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
    public void incrementStudentDiscountCount(Long customerId) {
        Customer customer = getById(customerId);
        customer.setStudentDiscountCount(customer.getStudentDiscountCount() + 1);
        customerRepository.save(customer);
    }
}
