package in.bulletbeats.domain.shared.enums;

public enum MenuItemType {
    DISH("Dish"),
    COMBO("Combo");

    private final String displayName;

    MenuItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
