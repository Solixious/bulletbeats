package in.bulletbeats.domain.billing.dto;

public record TablePlanDto(
        Long id,
        String name,
        Integer capacity,
        int seatCount,
        int xPos,
        int yPos,
        String status,
        boolean active,
        Long floorId,
        int tableWidth,
        int tableHeight,
        int rotation
) {}
