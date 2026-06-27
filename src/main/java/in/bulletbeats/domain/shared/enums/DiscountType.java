package in.bulletbeats.domain.shared.enums;

public enum DiscountType {

    FIXED("Fixed (₹)"),
    PERCENTAGE("Percentage (%)");

    private final String displayName;

    DiscountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
