package in.bulletbeats.domain.shared.exception;

public class UnitInUseException extends RuntimeException {
    public UnitInUseException(String name) {
        super("Unit \"" + name + "\" is used by inventory items or conversions and cannot be deleted");
    }
}
