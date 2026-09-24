package in.bulletbeats.domain.crm.repository;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.shared.enums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    @Query("SELECT c FROM Customer c WHERE c.phone = :local OR c.phone = :e164")
    Optional<Customer> findByNormalizedPhone(@Param("local") String local, @Param("e164") String e164);

    boolean existsByPhone(String phone);

    @Query("""
        SELECT c FROM Customer c
        WHERE lower(c.name) LIKE lower(concat('%', :query, '%'))
           OR c.phone LIKE concat('%', :query, '%')
        ORDER BY c.isVip DESC, c.name ASC
    """)
    List<Customer> search(@Param("query") String query);

    List<Customer> findAllByOrderByIsVipDescNameAsc();

    /**
     * Bucket for a new customer: fewest customers with a PAID bill first (that's the promo audience),
     * then fewest customers overall so a burst of not-yet-paid signups still round-robins.
     */
    @Query(value = """
        SELECT g.bucket FROM generate_series(1, 7) AS g(bucket)
        ORDER BY (SELECT COUNT(*) FROM customers c
                  WHERE c.promo_bucket = g.bucket
                    AND EXISTS (SELECT 1 FROM bills b WHERE b.customer_id = c.id AND b.status = 'PAID')),
                 (SELECT COUNT(*) FROM customers c WHERE c.promo_bucket = g.bucket),
                 g.bucket
        LIMIT 1
    """, nativeQuery = true)
    int findLeastPopulatedPromoBucket();

    @Query("""
        SELECT c FROM Customer c
        WHERE c.promoBucket = :bucket
          AND EXISTS (SELECT 1 FROM Bill b WHERE b.customer = c AND b.status = :paid)
        ORDER BY c.name ASC
    """)
    List<Customer> findPromoAudience(@Param("bucket") int bucket, @Param("paid") BillStatus paid);

    @Query("""
        SELECT c.promoBucket, COUNT(c) FROM Customer c
        WHERE EXISTS (SELECT 1 FROM Bill b WHERE b.customer = c AND b.status = :paid)
        GROUP BY c.promoBucket
    """)
    List<Object[]> countPromoAudienceByBucket(@Param("paid") BillStatus paid);
}
