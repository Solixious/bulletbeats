package in.bulletbeats.domain.shared.exception;

public class DuplicatePhoneException extends RuntimeException {

    public DuplicatePhoneException(String phone) {
        super("Customer with phone already exists: " + phone);
    }
}
