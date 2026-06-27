package in.bulletbeats.domain.billing.dto;

import in.bulletbeats.domain.shared.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PayBillDto {

    @NotNull
    private PaymentMethod method;

    private String referenceNote;
}
