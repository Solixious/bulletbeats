package in.bulletbeats.domain.shared.enums;

public enum ActorType {

    STAFF("Staff"),
    CUSTOMER("Customer"),
    SYSTEM("System");

    private final String displayName;

    ActorType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
