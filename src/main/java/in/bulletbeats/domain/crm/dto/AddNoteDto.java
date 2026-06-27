package in.bulletbeats.domain.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddNoteDto {

    @NotBlank
    @Size(max = 1000)
    private String note;
}
