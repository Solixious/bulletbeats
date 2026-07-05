package in.bulletbeats.domain.billing.dto;

import in.bulletbeats.domain.shared.enums.OnlineOrderPlatform;
import in.bulletbeats.domain.shared.enums.OrderType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateBillDto {

    @NotNull
    private OrderType orderType;

    private Long cafeTableId;

    private OnlineOrderPlatform onlineOrderPlatform;

    private String customerPhone;

    private String customerName;
}
