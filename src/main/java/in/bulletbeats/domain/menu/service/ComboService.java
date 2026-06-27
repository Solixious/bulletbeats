package in.bulletbeats.domain.menu.service;

import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.repository.GroceryItemRepository;
import in.bulletbeats.domain.menu.dto.ComboIngredientDto;
import in.bulletbeats.domain.menu.dto.CreateComboDto;
import in.bulletbeats.domain.menu.dto.UpdateComboDto;
import in.bulletbeats.domain.menu.entity.Combo;
import in.bulletbeats.domain.menu.entity.ComboIngredient;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.repository.ComboRepository;
import in.bulletbeats.domain.menu.repository.MenuItemRepository;
import in.bulletbeats.domain.shared.exception.ComboInUseException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComboService {

    private final ComboRepository comboRepository;
    private final GroceryItemRepository groceryItemRepository;
    private final MenuItemRepository menuItemRepository;

    public List<Combo> getAll() {
        return comboRepository.findByIsActiveTrueOrderByNameAsc();
    }

    public Combo getById(Long id) {
        return comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Combo not found with id: " + id));
    }

    @Transactional
    public Combo create(CreateComboDto dto) {
        if (comboRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("A combo with this name already exists: " + dto.getName());
        }
        Combo combo = Combo.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .isActive(true)
                .tenantId(1L)
                .build();
        dto.getIngredients().forEach(i -> combo.getIngredients().add(buildIngredient(combo, i)));
        return comboRepository.save(combo);
    }

    @Transactional
    public Combo update(Long id, UpdateComboDto dto) {
        Combo combo = getById(id);
        if (!combo.getName().equalsIgnoreCase(dto.getName())
                && comboRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("A combo with this name already exists: " + dto.getName());
        }
        combo.setName(dto.getName());
        combo.setDescription(dto.getDescription());
        combo.getIngredients().clear();
        dto.getIngredients().forEach(i -> combo.getIngredients().add(buildIngredient(combo, i)));
        return comboRepository.save(combo);
    }

    @Transactional
    public void deactivate(Long id) {
        Combo combo = getById(id);
        boolean inUse = menuItemRepository.findByComboId(id).stream().anyMatch(MenuItem::isActive);
        if (inUse) {
            throw new ComboInUseException(id);
        }
        combo.setActive(false);
        comboRepository.save(combo);
    }

    private ComboIngredient buildIngredient(Combo combo, ComboIngredientDto dto) {
        GroceryItem groceryItem = groceryItemRepository.findById(dto.getGroceryItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grocery item not found with id: " + dto.getGroceryItemId()));
        return ComboIngredient.builder()
                .combo(combo)
                .groceryItem(groceryItem)
                .quantityRequired(dto.getQuantityRequired())
                .build();
    }
}
