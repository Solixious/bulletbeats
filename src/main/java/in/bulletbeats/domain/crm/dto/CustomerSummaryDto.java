package in.bulletbeats.domain.crm.dto;

import in.bulletbeats.domain.crm.entity.Customer;

public record CustomerSummaryDto(
        Long id,
        String name,
        String phone,
        int loyaltyPoints,
        boolean isVip
) {
    public static CustomerSummaryDto from(Customer c) {
        return new CustomerSummaryDto(
                c.getId(),
                c.getName(),
                c.getPhone(),
                c.getLoyaltyPoints(),
                c.isVip()
        );
    }
}
