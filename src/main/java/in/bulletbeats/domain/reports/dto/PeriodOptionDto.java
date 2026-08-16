package in.bulletbeats.domain.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One choice in a report period dropdown — value is "ALL" or an ISO yyyy-MM month. */
@Data
@AllArgsConstructor
public class PeriodOptionDto {
    private String value;
    private String label;
}
