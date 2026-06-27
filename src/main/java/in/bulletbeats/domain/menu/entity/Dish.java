package in.bulletbeats.domain.menu.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dishes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dish extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String recipeNotes;

    private Integer prepTimeMinutes;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private Long tenantId;

    @Builder.Default
    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DishIngredient> ingredients = new ArrayList<>();
}
