package in.bulletbeats.domain.shared.enums;

public enum PaymentMethod {

    CASH("Cash"),
    CARD("Card"),
    UPI("UPI"),
    OTHER("Other");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
