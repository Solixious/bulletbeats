package in.bulletbeats.domain.menu.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import in.bulletbeats.domain.shared.enums.MenuItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItem extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id")
    private Dish dish;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    private Combo combo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean isAvailable;

    private Boolean availabilityOverride;

    @Column(length = 255)
    private String imagePath;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private Long tenantId;

    public MenuItemType getType() {
        return dish != null ? MenuItemType.DISH : MenuItemType.COMBO;
    }
}
