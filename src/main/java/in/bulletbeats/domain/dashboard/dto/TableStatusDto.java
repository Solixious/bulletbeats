package in.bulletbeats.domain.dashboard.dto;

import in.bulletbeats.domain.shared.enums.TableStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableStatusDto {
    private Long tableId;
    private String tableName;
    private TableStatus status;
    private long activeBillCount;
    private String activeCustomerName;
}
