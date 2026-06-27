package in.bulletbeats.domain.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CafeTableDto {

    @NotBlank
    @Size(max = 50)
    private String name;

    private Integer capacity;
}
