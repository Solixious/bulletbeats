package in.bulletbeats.domain.billing.dto;

import java.util.List;

public record FloorWithTablesDto(
        Long id,
        String name,
        int displayOrder,
        int width,
        int height,
        List<TablePlanDto> tables,
        List<FloorElementDto> elements
) {}
