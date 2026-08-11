package in.bulletbeats.domain.offers.dto;

import in.bulletbeats.domain.offers.entity.enums.CodeUsageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OfferCodeDto {

    private String code;

    /** Restricts the code to one existing customer, looked up by phone. */
    private String customerPhone;

    @NotNull
    private CodeUsageType usageType;

    private Integer maxUses;

    private LocalDateTime expiresAt;
}
