package in.bulletbeats.domain.menu.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CreateMenuItemDto {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotNull
    private Long categoryId;

    private String description;

    private Long dishId;

    private Long comboId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    @Size(max = 40)
    private String quantityLabel;

    private int displayOrder;

    /** Optional per-platform price override, keyed by online platform id; blank/absent means "use base price". */
    private Map<Long, String> platformPrices = new LinkedHashMap<>();

    @AssertTrue(message = "Exactly one of dish or combo must be selected")
    public boolean isDishOrComboSelected() {
        return (dishId == null) != (comboId == null);
    }
}
