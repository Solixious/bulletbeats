package in.bulletbeats.domain.billing.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QrConfirmDto {
    private Long billId;
    private String customerName;
    private Long customerId;
}
