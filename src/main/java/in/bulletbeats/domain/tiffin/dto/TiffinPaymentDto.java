package in.bulletbeats.domain.tiffin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TiffinPaymentDto {

    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.01", message = "Amount paid must be greater than zero")
    private BigDecimal amountPaid;

    @NotNull(message = "Coverage start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate coverageFrom;

    @NotNull(message = "Coverage end date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate coverageUntil;

    @NotNull(message = "Paid on date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paidOn = LocalDate.now();

    private String note;
}
