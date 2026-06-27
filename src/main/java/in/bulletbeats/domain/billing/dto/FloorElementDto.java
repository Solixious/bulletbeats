package in.bulletbeats.domain.billing.dto;

public record FloorElementDto(
        Long id,
        String elementType,
        String label,
        int xPos,
        int yPos,
        Long floorId,
        Integer x2,
        Integer y2,
        int width,
        Integer height,
        int rotation
) {}
