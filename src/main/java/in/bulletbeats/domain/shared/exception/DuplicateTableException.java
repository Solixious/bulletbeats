package in.bulletbeats.domain.shared.exception;

public class DuplicateTableException extends RuntimeException {

    public DuplicateTableException(String name) {
        super("A table with name '" + name + "' already exists");
    }
}
