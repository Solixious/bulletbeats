package in.bulletbeats.domain.shared.enums;

public enum MovementType {

    INBOUND("Stock In"),
    OUTBOUND("Stock Out"),
    ADJUSTMENT("Manual Adjustment"),
    WASTAGE("Wastage");

    private final String displayName;

    MovementType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
