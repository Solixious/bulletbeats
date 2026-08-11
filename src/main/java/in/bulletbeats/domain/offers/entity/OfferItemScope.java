package in.bulletbeats.domain.offers.entity;

import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.entity.MenuItem;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "offer_item_scopes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferItemScope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    public boolean matches(MenuItem item) {
        if (menuItem != null) {
            return menuItem.getId().equals(item.getId());
        }
        if (category != null) {
            return item.getCategory() != null && category.getId().equals(item.getCategory().getId());
        }
        return false;
    }
}
