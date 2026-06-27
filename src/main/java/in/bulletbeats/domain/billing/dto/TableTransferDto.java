package in.bulletbeats.domain.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableTransferDto {

    @NotNull
    private Long toTableId;
}
