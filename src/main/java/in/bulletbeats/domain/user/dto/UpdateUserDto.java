package in.bulletbeats.domain.user.dto;

import in.bulletbeats.domain.shared.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserDto {

    @NotBlank
    private String fullName;

    @NotNull
    private Role role;

    private boolean active;
}
