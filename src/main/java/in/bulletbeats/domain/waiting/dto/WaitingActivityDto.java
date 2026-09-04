package in.bulletbeats.domain.waiting.dto;

import in.bulletbeats.domain.waiting.entity.WaitingActivityCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WaitingActivityDto {
    private WaitingActivityCategory category;
    private String name;
    private int sortOrder;
}
