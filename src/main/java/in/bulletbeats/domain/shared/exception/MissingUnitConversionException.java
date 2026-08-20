package in.bulletbeats.domain.shared.exception;

public class MissingUnitConversionException extends RuntimeException {
    public MissingUnitConversionException(String fromUnit, String toUnit) {
        super("No unit conversion defined between \"" + fromUnit + "\" and \"" + toUnit + "\"");
    }
}
