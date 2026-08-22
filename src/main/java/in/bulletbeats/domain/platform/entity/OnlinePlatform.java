package in.bulletbeats.domain.platform.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "online_platforms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlinePlatform extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Long tenantId = 1L;
}
