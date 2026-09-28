package in.bulletbeats.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One ISO week (Mon–Sun) of customer activity, serialized to JSON for the
 * dashboard's client-side trend charts. Dates are ISO strings so the payload
 * doesn't depend on the JSON serializer's java.time support.
 *
 * <p>Activity: {@code newCustomers}/{@code existingCustomers} are distinct named
 * customers with a PAID bill that week; {@code anonymousOrders} counts PAID bills
 * with no customer (each one counted as one customer, since they can't be
 * de-duplicated); {@code totalCustomers} is the sum of all three.
 *
 * <p>Retention: of this week's new/existing named customers, how many placed a
 * PAID bill again the following week. Anonymous orders are excluded. Retention
 * rates are null when the cohort is empty, and all retention fields are absent
 * ({@code hasRetention == false}) for the current week, whose follow-up week
 * hasn't started.
 */
@Data
@AllArgsConstructor
public class CustomerWeekPointDto {
    private String weekStart;
    private String label;
    private String rangeLabel;
    private String nextWeekRangeLabel;

    private long newCustomers;
    private long existingCustomers;
    private long anonymousOrders;
    private long totalCustomers;
    private boolean activityPartial;

    private boolean hasRetention;
    private boolean retentionPartial;
    private long newRetained;
    private long existingRetained;
    private Double newRetentionPercent;
    private Double existingRetentionPercent;
}
