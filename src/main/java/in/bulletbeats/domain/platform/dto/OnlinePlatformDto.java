package in.bulletbeats.domain.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OnlinePlatformDto {

    @NotBlank
    @Size(max = 100)
    private String name;
}
