package in.bulletbeats.domain.shared.enums;

public enum BillStatus {

    DRAFT("Draft"),
    CONFIRMED("Confirmed"),
    PAID("Paid"),
    CANCELLED("Cancelled");

    private final String displayName;

    BillStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == PAID || this == CANCELLED;
    }

    public boolean canAddItems() {
        return this == DRAFT;
    }
}
