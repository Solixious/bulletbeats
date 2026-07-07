package in.bulletbeats.domain.tiffin.repository;

import in.bulletbeats.domain.tiffin.TiffinMealType;
import in.bulletbeats.domain.tiffin.entity.TiffinPricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TiffinPricingRepository extends JpaRepository<TiffinPricing, Long> {
    Optional<TiffinPricing> findByMealType(TiffinMealType mealType);
}
