package in.bulletbeats.domain.shared.exception;

public class DuplicateUnitConversionException extends RuntimeException {
    public DuplicateUnitConversionException(String fromUnit, String toUnit) {
        super("A conversion from \"" + fromUnit + "\" to \"" + toUnit + "\" already exists");
    }
}
