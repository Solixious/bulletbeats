package in.bulletbeats.domain.menu.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AvailabilityOverrideDto {

    // null = auto, true = force-on, false = force-off
    private Boolean override;

    @Size(max = 255)
    private String reason;
}
