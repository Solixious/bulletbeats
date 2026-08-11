package in.bulletbeats.domain.shared.exception;

public class OfferException extends RuntimeException {

    private final String reason;

    public OfferException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
