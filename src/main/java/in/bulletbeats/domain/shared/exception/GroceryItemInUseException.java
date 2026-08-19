package in.bulletbeats.domain.shared.exception;

public class GroceryItemInUseException extends RuntimeException {
    public GroceryItemInUseException(String itemName) {
        super("\"" + itemName + "\" has stock, purchase order, or recipe history and cannot be deleted");
    }
}
