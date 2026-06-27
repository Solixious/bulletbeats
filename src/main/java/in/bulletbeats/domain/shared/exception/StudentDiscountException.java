package in.bulletbeats.domain.shared.exception;

public class StudentDiscountException extends RuntimeException {

    private final String reason;

    public StudentDiscountException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
