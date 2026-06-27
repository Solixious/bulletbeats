package in.bulletbeats.domain.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateBillDto {

    @NotNull
    private Long cafeTableId;

    private String customerPhone;

    private String customerName;
}
