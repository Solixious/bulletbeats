package in.bulletbeats.domain.billing.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "floor_elements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloorElement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long floorId;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String elementType = "CASH_COUNTER";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String label = "CASH";

    @Column(nullable = false)
    @Builder.Default
    private int xPos = 60;

    @Column(nullable = false)
    @Builder.Default
    private int yPos = 60;

    /** WALL: relative end-point vector (dx). DOOR/others: unused. */
    private Integer x2;

    /** WALL: relative end-point vector (dy). DOOR/others: unused. */
    private Integer y2;

    /** DOOR: opening width in px. CASH_COUNTER: element width. Others: unused. */
    @Column(nullable = false)
    @Builder.Default
    private int width = 60;

    /** CASH_COUNTER: element height in px. Others: unused. */
    private Integer height;

    /** DOOR/CASH_COUNTER: rotation in degrees (0/90/180/270). Others: unused. */
    @Column(nullable = false)
    @Builder.Default
    private int rotation = 0;
}
