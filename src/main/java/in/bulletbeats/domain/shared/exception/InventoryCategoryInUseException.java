package in.bulletbeats.domain.shared.exception;

public class InventoryCategoryInUseException extends RuntimeException {
    public InventoryCategoryInUseException(String name) {
        super("Category \"" + name + "\" has inventory items assigned to it and cannot be deleted");
    }
}
