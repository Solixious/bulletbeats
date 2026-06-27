package in.bulletbeats.domain.user.dto;

import in.bulletbeats.domain.shared.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserDto {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    private String fullName;

    @NotBlank
    @Size(min = 6)
    private String password;

    @NotNull
    private Role role;
}
