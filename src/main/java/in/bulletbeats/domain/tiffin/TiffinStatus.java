package in.bulletbeats.domain.tiffin;

public enum TiffinStatus {
    ACTIVE("Active"),
    PAUSED("Paused"),
    CANCELLED("Cancelled");

    private final String displayName;

    TiffinStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
