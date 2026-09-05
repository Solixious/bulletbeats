package in.bulletbeats.domain.inventory.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCategory extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private Long tenantId;
}
