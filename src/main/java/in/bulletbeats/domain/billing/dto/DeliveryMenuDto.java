package in.bulletbeats.domain.billing.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class DeliveryMenuDto {
    private final Long billId;
    private final String deliveryAddress;
    private final String customerName;
    private final List<CategoryWithItemsDto> categories;
}
