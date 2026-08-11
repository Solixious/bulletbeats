package in.bulletbeats.domain.offers.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CustomerCohortDto {

    @NotBlank
    private String name;

    private String description;

    private List<CohortRuleDto> rules = new ArrayList<>();
}
