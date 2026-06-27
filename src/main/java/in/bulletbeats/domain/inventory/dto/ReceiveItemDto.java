package in.bulletbeats.domain.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReceiveItemDto {

    private Long purchaseOrderItemId;
    private BigDecimal quantityReceived;
}
