package in.bulletbeats.domain.offers.dto;

import in.bulletbeats.domain.offers.entity.enums.CohortRuleField;
import in.bulletbeats.domain.offers.entity.enums.CohortRuleOperator;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CohortRuleDto {

    private CohortRuleField field;

    private CohortRuleOperator operator;

    private BigDecimal value;

    private Integer periodDays;
}
