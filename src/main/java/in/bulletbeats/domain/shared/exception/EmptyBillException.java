package in.bulletbeats.domain.shared.exception;

public class EmptyBillException extends RuntimeException {

    public EmptyBillException() {
        super("Bill has no items");
    }
}
