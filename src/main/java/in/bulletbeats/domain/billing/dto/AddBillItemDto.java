package in.bulletbeats.domain.billing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddBillItemDto {

    @NotNull
    private Long menuItemId;

    @Min(1)
    private int quantity = 1;
}
