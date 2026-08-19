package in.bulletbeats.domain.inventory.util;

import java.math.BigDecimal;
import java.util.Map;

public final class UnitConversions {

    private static final Map<String, BigDecimal> FACTORS = Map.ofEntries(
            Map.entry("kg->g", BigDecimal.valueOf(1000)),
            Map.entry("kilogram->gram", BigDecimal.valueOf(1000)),
            Map.entry("g->kg", BigDecimal.valueOf(0.001)),
            Map.entry("gram->kilogram", BigDecimal.valueOf(0.001)),
            Map.entry("l->ml", BigDecimal.valueOf(1000)),
            Map.entry("litre->ml", BigDecimal.valueOf(1000)),
            Map.entry("liter->ml", BigDecimal.valueOf(1000)),
            Map.entry("ml->l", BigDecimal.valueOf(0.001)),
            Map.entry("ml->litre", BigDecimal.valueOf(0.001)),
            Map.entry("ml->liter", BigDecimal.valueOf(0.001)),
            Map.entry("dozen->piece", BigDecimal.valueOf(12)),
            Map.entry("dozen->pcs", BigDecimal.valueOf(12)),
            Map.entry("dozen->pc", BigDecimal.valueOf(12)),
            Map.entry("dozen->unit", BigDecimal.valueOf(12))
    );

    private UnitConversions() {
    }

    /**
     * Multiplier to convert a value expressed in fromUnit to toUnit, or null if the
     * pair isn't a known conversion (in which case per-minor-unit cost can't be derived).
     */
    public static BigDecimal factor(String fromUnit, String toUnit) {
        if (fromUnit == null || toUnit == null) return null;
        String from = fromUnit.trim().toLowerCase();
        String to = toUnit.trim().toLowerCase();
        if (from.equals(to)) return BigDecimal.ONE;
        return FACTORS.get(from + "->" + to);
    }
}
