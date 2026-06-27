package in.bulletbeats.domain.shared.enums;

public enum LoyaltyTransactionType {

    EARN("Earned"),
    REDEEM("Redeemed"),
    ADJUST("Adjusted");

    private final String displayName;

    LoyaltyTransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
