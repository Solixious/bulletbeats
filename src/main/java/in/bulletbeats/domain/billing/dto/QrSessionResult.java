package in.bulletbeats.domain.billing.dto;

import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.crm.entity.Customer;

public record QrSessionResult(
        CafeTable table,
        Bill bill,
        Customer customer,
        boolean isNewSession,
        boolean isReturningCustomer
) {}
