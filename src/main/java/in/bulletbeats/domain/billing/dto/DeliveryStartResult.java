package in.bulletbeats.domain.billing.dto;

import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.crm.entity.Customer;

public record DeliveryStartResult(
        Bill bill,
        Customer customer,
        boolean isReturningCustomer
) {}
