package in.bulletbeats.domain.shared.exception;

public class DishInUseException extends RuntimeException {
    public DishInUseException(Long dishId) {
        super("Dish " + dishId + " is used by active menu items and cannot be deactivated");
    }
}
