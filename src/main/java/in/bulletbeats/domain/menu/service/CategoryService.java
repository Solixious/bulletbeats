package in.bulletbeats.domain.menu.service;

import in.bulletbeats.domain.menu.dto.CategoryDto;
import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.repository.CategoryRepository;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAllActive() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    public List<Category> getAllInactive() {
        return categoryRepository.findByIsActiveFalseOrderByNameAsc();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Transactional
    public Category create(CategoryDto dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Category already exists: " + dto.getName());
        }
        Category category = Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .displayOrder(dto.getDisplayOrder())
                .isActive(true)
                .tenantId(1L)
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryDto dto) {
        Category category = getById(id);
        if (!category.getName().equalsIgnoreCase(dto.getName())
                && categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Category already exists: " + dto.getName());
        }
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setDisplayOrder(dto.getDisplayOrder());
        return categoryRepository.save(category);
    }

    @Transactional
    public void reorder(List<Long> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            Category category = getById(orderedIds.get(i));
            category.setDisplayOrder(i);
            categoryRepository.save(category);
        }
    }

    @Transactional
    public void deactivate(Long id) {
        Category category = getById(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    @Transactional
    public void reactivate(Long id) {
        Category category = getById(id);
        category.setActive(true);
        categoryRepository.save(category);
    }
}
