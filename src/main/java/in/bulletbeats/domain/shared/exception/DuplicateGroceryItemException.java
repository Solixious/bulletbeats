package in.bulletbeats.domain.shared.exception;

public class DuplicateGroceryItemException extends RuntimeException {

    public DuplicateGroceryItemException(String name) {
        super("Grocery item already exists: " + name);
    }
}
