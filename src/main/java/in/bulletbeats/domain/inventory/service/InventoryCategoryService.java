package in.bulletbeats.domain.inventory.service;

import in.bulletbeats.domain.inventory.dto.InventoryCategoryDto;
import in.bulletbeats.domain.inventory.entity.InventoryCategory;
import in.bulletbeats.domain.inventory.repository.GroceryItemRepository;
import in.bulletbeats.domain.inventory.repository.InventoryCategoryRepository;
import in.bulletbeats.domain.shared.exception.InventoryCategoryInUseException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryCategoryService {

    private final InventoryCategoryRepository categoryRepository;
    private final GroceryItemRepository groceryItemRepository;

    public List<InventoryCategory> getAll() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    public InventoryCategory getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory category not found with id: " + id));
    }

    @Transactional
    public InventoryCategory create(InventoryCategoryDto dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Category already exists: " + dto.getName());
        }
        InventoryCategory category = InventoryCategory.builder()
                .name(dto.getName())
                .displayOrder(dto.getDisplayOrder())
                .tenantId(1L)
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public InventoryCategory update(Long id, InventoryCategoryDto dto) {
        InventoryCategory category = getById(id);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            throw new IllegalArgumentException("Category already exists: " + dto.getName());
        }
        category.setName(dto.getName());
        category.setDisplayOrder(dto.getDisplayOrder());
        return categoryRepository.save(category);
    }

    @Transactional
    public void reorder(List<Long> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            InventoryCategory category = getById(orderedIds.get(i));
            category.setDisplayOrder(i);
            categoryRepository.save(category);
        }
    }

    @Transactional
    public void delete(Long id) {
        InventoryCategory category = getById(id);
        if (groceryItemRepository.existsByCategoryId(id)) {
            throw new InventoryCategoryInUseException(category.getName());
        }
        categoryRepository.delete(category);
    }
}
