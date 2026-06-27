package in.bulletbeats.domain.shared.exception;

public class SupplierAlreadyExistsException extends RuntimeException {

    public SupplierAlreadyExistsException(String name) {
        super("Supplier already exists: " + name);
    }
}
