package in.bulletbeats.domain.shared.exception;

public class TableOccupiedException extends RuntimeException {

    public TableOccupiedException(String message) {
        super(message);
    }
}
