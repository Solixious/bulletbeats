package in.bulletbeats.domain.shared.exception;

public class DuplicatePlatformException extends RuntimeException {

    public DuplicatePlatformException(String name) {
        super("An online platform with name '" + name + "' already exists");
    }
}
