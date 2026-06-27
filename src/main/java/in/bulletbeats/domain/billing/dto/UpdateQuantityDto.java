package in.bulletbeats.domain.billing.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateQuantityDto {

    @Min(0)
    private int quantity;
}
