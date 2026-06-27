package in.bulletbeats.domain.shared.exception;

import java.util.List;

public class InsufficientStockException extends RuntimeException {

    private final List<String> details;

    public InsufficientStockException(List<String> details) {
        super("Insufficient stock");
        this.details = details;
    }

    public List<String> getDetails() {
        return details;
    }
}
