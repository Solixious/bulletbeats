package in.bulletbeats.domain.tiffin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class TiffinSubscriptionDto {

    @NotNull(message = "Please select a customer")
    private Long customerId;

    private String customerName;

    private boolean hasBreakfast;
    private boolean hasLunch;
    private boolean hasDinner;

    private boolean allDays = true;

    private List<String> selectedDays = new ArrayList<>();

    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate = LocalDate.now();

    public String getCustomDaysString() {
        if (selectedDays == null || selectedDays.isEmpty()) return null;
        return String.join(",", selectedDays);
    }
}
