package in.bulletbeats.domain.shared.exception;

public class NoBillsToTransferException extends RuntimeException {

    public NoBillsToTransferException(String message) {
        super(message);
    }
}
