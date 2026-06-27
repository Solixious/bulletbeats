package in.bulletbeats.domain.billing.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class QrMenuDto {
    private final Long billId;
    private final String tableName;
    private final String customerName;
    private final boolean returningCustomer;
    private final boolean confirmed;
    private final List<CategoryWithItemsDto> categories;
}
