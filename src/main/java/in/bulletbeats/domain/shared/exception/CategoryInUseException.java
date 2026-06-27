package in.bulletbeats.domain.shared.exception;

public class CategoryInUseException extends RuntimeException {
    public CategoryInUseException(Long categoryId) {
        super("Category " + categoryId + " has active menu items and cannot be deactivated");
    }
}
