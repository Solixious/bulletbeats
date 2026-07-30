package in.bulletbeats.domain.shared.exception;

public class CategoryInUseException extends RuntimeException {
    public CategoryInUseException(Long categoryId) {
        super("Category " + categoryId + " has menu items assigned to it and cannot be removed");
    }
}
