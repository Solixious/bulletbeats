package in.bulletbeats.domain.billing.dto;

import in.bulletbeats.domain.billing.entity.CafeTable;

public record TableTransferResult(
        CafeTable fromTable,
        CafeTable toTable,
        int billsTransferred
) {}
