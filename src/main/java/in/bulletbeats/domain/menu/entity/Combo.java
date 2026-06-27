package in.bulletbeats.domain.menu.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "combos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Combo extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private Long tenantId;

    @Builder.Default
    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ComboIngredient> ingredients = new ArrayList<>();
}
