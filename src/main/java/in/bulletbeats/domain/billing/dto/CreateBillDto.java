package in.bulletbeats.domain.billing.dto;

import in.bulletbeats.domain.shared.enums.OrderType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateBillDto {

    @NotNull
    private OrderType orderType;

    private Long cafeTableId;

    private Long onlinePlatformId;

    private String customerPhone;

    private String customerName;

    /** Admin-only: backdate the bill to a previous day. Ignored for non-admins. */
    @PastOrPresent(message = "Bill date cannot be in the future")
    private LocalDate billDate;
}
