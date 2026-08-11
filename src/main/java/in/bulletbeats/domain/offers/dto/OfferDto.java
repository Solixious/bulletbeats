package in.bulletbeats.domain.offers.dto;

import in.bulletbeats.domain.offers.entity.enums.OfferMechanism;
import in.bulletbeats.domain.offers.entity.enums.OfferTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class OfferDto {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private OfferMechanism mechanism;

    private BigDecimal percentageValue;

    private BigDecimal fixedValue;

    private Integer buyQuantity;

    private Integer getQuantity;

    @NotNull
    private OfferTargetType targetType = OfferTargetType.ALL;

    private Long cohortId;

    /** Looked up against an existing Customer by phone — used only when targetType=CUSTOMER. */
    private String customerPhone;

    private BigDecimal minSpend = BigDecimal.ZERO;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    private boolean requiresCode;

    private Integer maxTotalUses;

    private Integer maxUsesPerCustomer;

    private boolean active = true;

    /** Used only for ITEM_PERCENTAGE / ITEM_FIXED / BUY_X_GET_Y_FREE mechanisms. */
    private List<Long> menuItemIds = new ArrayList<>();

    private List<Long> categoryIds = new ArrayList<>();
}
