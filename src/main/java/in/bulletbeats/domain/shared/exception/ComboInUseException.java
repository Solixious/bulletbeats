package in.bulletbeats.domain.shared.exception;

public class ComboInUseException extends RuntimeException {
    public ComboInUseException(Long comboId) {
        super("Combo " + comboId + " is used by active menu items and cannot be deactivated");
    }
}
