package in.bulletbeats.domain.shared.exception;

public class PreparedItemInUseException extends RuntimeException {
    public PreparedItemInUseException(String itemName) {
        super("\"" + itemName + "\" has stock history or is used in a recipe and cannot be deleted");
    }
}
