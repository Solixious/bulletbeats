package in.bulletbeats.domain.shared.enums;

public enum DishType {
    VEG("Veg"),
    NON_VEG("Non-Veg"),
    EGG("Egg");

    private final String displayName;

    DishType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
