package in.bulletbeats.domain.shared.enums;

public enum TableStatus {

    FREE("Free"),
    OCCUPIED("Occupied");

    private final String displayName;

    TableStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
