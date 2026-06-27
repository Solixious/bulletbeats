package in.bulletbeats.domain.shared.exception;

public class InvalidMenuItemException extends RuntimeException {
    public InvalidMenuItemException(String message) {
        super(message);
    }
}
