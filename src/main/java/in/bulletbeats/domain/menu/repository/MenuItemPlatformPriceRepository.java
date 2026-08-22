package in.bulletbeats.domain.menu.repository;

import in.bulletbeats.domain.menu.entity.MenuItemPlatformPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuItemPlatformPriceRepository extends JpaRepository<MenuItemPlatformPrice, Long> {

    List<MenuItemPlatformPrice> findByMenuItemId(Long menuItemId);

    Optional<MenuItemPlatformPrice> findByMenuItemIdAndPlatformId(Long menuItemId, Long platformId);
}
