package in.bulletbeats.domain.shared.enums;

public enum OnlineOrderPlatform {

    ZOMATO("Zomato"),
    SWIGGY("Swiggy");

    private final String displayName;

    OnlineOrderPlatform(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
