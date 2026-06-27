package in.bulletbeats.domain.billing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QrAddItemDto {

    @NotNull
    private Long billId;

    @NotNull
    private Long menuItemId;

    @Min(1)
    private int quantity = 1;

    private String customerName;

    private Long customerId;
}
