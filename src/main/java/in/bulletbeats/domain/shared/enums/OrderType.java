package in.bulletbeats.domain.shared.enums;

public enum OrderType {

    DINE_IN("Dine-In"),
    TAKEAWAY("Takeaway"),
    ONLINE_ORDER("Online Order"),
    DIRECT_DELIVERY("Direct Delivery");

    private final String displayName;

    OrderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
