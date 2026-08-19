package in.bulletbeats.domain.shared.exception;

public class UnitAlreadyExistsException extends RuntimeException {
    public UnitAlreadyExistsException(String name) {
        super("Unit \"" + name + "\" already exists");
    }
}
