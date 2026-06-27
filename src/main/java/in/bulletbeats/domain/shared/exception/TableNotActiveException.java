package in.bulletbeats.domain.shared.exception;

public class TableNotActiveException extends RuntimeException {

    public TableNotActiveException(String message) {
        super(message);
    }
}
