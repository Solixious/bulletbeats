package in.bulletbeats.domain.crm.repository;

import in.bulletbeats.domain.crm.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    boolean existsByPhone(String phone);

    @Query("""
        SELECT c FROM Customer c
        WHERE lower(c.name) LIKE lower(concat('%', :query, '%'))
           OR c.phone LIKE concat('%', :query, '%')
        ORDER BY c.isVip DESC, c.name ASC
    """)
    List<Customer> search(@Param("query") String query);

    List<Customer> findAllByOrderByIsVipDescNameAsc();
}
