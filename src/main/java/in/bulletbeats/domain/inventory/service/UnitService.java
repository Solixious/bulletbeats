package in.bulletbeats.domain.inventory.service;

import in.bulletbeats.domain.inventory.dto.UnitConversionDto;
import in.bulletbeats.domain.inventory.dto.UnitDto;
import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.entity.Unit;
import in.bulletbeats.domain.inventory.entity.UnitConversion;
import in.bulletbeats.domain.inventory.repository.GroceryItemRepository;
import in.bulletbeats.domain.inventory.repository.UnitConversionRepository;
import in.bulletbeats.domain.inventory.repository.UnitRepository;
import in.bulletbeats.domain.shared.exception.DuplicateUnitConversionException;
import in.bulletbeats.domain.shared.exception.MissingUnitConversionException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import in.bulletbeats.domain.shared.exception.UnitAlreadyExistsException;
import in.bulletbeats.domain.shared.exception.UnitInUseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnitService {

    private final UnitRepository unitRepository;
    private final UnitConversionRepository unitConversionRepository;
    private final GroceryItemRepository groceryItemRepository;

    public List<Unit> getAllUnits() {
        return unitRepository.findAllByOrderByNameAsc();
    }

    public List<UnitConversion> getAllConversions() {
        return unitConversionRepository.findAllWithUnits();
    }

    public Unit getUnitById(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + id));
    }

    @Transactional
    public Unit createUnit(UnitDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Unit name is required");
        }
        String name = dto.getName().trim().toLowerCase();
        if (unitRepository.existsByNameIgnoreCase(name)) {
            throw new UnitAlreadyExistsException(name);
        }
        return unitRepository.save(Unit.builder().name(name).build());
    }

    @Transactional
    public void deleteUnit(Long id) {
        Unit unit = getUnitById(id);
        boolean inUse = groceryItemRepository.existsByAnyUnitField(unit.getName())
                || unitConversionRepository.existsByFromUnitIdOrToUnitId(id, id);
        if (inUse) {
            throw new UnitInUseException(unit.getName());
        }
        unitRepository.delete(unit);
    }

    @Transactional
    public UnitConversion createConversion(UnitConversionDto dto) {
        if (dto.getFromUnitId() == null || dto.getToUnitId() == null || dto.getFactor() == null) {
            throw new IllegalArgumentException("From unit, to unit, and factor are all required");
        }
        if (dto.getFromUnitId().equals(dto.getToUnitId())) {
            throw new IllegalArgumentException("From and to units must be different");
        }
        if (dto.getFactor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Factor must be greater than zero");
        }
        Unit from = getUnitById(dto.getFromUnitId());
        Unit to = getUnitById(dto.getToUnitId());
        if (unitConversionRepository.existsByFromUnitIdAndToUnitId(from.getId(), to.getId())) {
            throw new DuplicateUnitConversionException(from.getName(), to.getName());
        }
        UnitConversion conversion = UnitConversion.builder()
                .fromUnit(from)
                .toUnit(to)
                .factor(dto.getFactor())
                .build();
        return unitConversionRepository.save(conversion);
    }

    @Transactional
    public void deleteConversion(Long id) {
        unitConversionRepository.deleteById(id);
    }

    /**
     * Multiplier to convert a value in fromUnitName to toUnitName, checking both the
     * stored direction and its inverse. Null if no conversion is defined for the pair.
     */
    public BigDecimal conversionFactor(String fromUnitName, String toUnitName) {
        if (fromUnitName == null || toUnitName == null) return null;
        if (fromUnitName.equalsIgnoreCase(toUnitName)) return BigDecimal.ONE;
        return unitConversionRepository.findByUnitNames(fromUnitName, toUnitName)
                .map(UnitConversion::getFactor)
                .or(() -> unitConversionRepository.findByUnitNames(toUnitName, fromUnitName)
                        .map(uc -> BigDecimal.ONE.divide(uc.getFactor(), 8, RoundingMode.HALF_UP)))
                .orElse(null);
    }

    /**
     * Converts a quantity from fromUnitName to toUnitName.
     * Throws MissingUnitConversionException if no conversion path is defined for the pair.
     */
    public BigDecimal convert(BigDecimal quantity, String fromUnitName, String toUnitName) {
        BigDecimal factor = conversionFactor(fromUnitName, toUnitName);
        if (factor == null) {
            throw new MissingUnitConversionException(fromUnitName, toUnitName);
        }
        return quantity.multiply(factor);
    }

    /**
     * Converts a recipe quantity (expressed in the grocery item's recipe/minor unit) into its stock unit.
     * No-op if the recipe unit and stock unit are the same.
     */
    public BigDecimal toStockUnit(GroceryItem item, BigDecimal recipeQuantity) {
        String recipeUnit = item.getRecipeUnit();
        if (recipeUnit.equalsIgnoreCase(item.getUnit())) {
            return recipeQuantity;
        }
        return convert(recipeQuantity, recipeUnit, item.getUnit());
    }
}
