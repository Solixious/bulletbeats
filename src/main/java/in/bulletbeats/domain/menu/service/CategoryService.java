package in.bulletbeats.domain.menu.service;

import in.bulletbeats.domain.menu.dto.CategoryDto;
import in.bulletbeats.domain.menu.dto.CategoryNode;
import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.repository.CategoryRepository;
import in.bulletbeats.domain.menu.repository.MenuItemRepository;
import in.bulletbeats.domain.shared.exception.CategoryInUseException;
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
    private final MenuItemRepository menuItemRepository;

    public List<Category> getAllActive() {
        return categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    public List<Category> getAllInactive() {
        return categoryRepository.findByParentIsNullAndIsActiveFalseOrderByNameAsc();
    }

    public List<Category> getTopLevelCategories() {
        return categoryRepository.findByParentIsNullOrderByDisplayOrderAscNameAsc();
    }

    public List<Category> getActiveSubcategories(Long parentId) {
        return categoryRepository.findByParentIdAndIsActiveTrueOrderByDisplayOrderAscNameAsc(parentId);
    }

    public List<Category> getAllSubcategories(Long parentId) {
        return categoryRepository.findByParentIdOrderByDisplayOrderAscNameAsc(parentId);
    }

    public List<CategoryNode> getActiveCategoryTree() {
        return getAllActive().stream()
                .map(cat -> new CategoryNode(cat, getActiveSubcategories(cat.getId())))
                .toList();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Transactional
    public Category create(CategoryDto dto) {
        Category parent = resolveParent(dto.getParentId(), null);
        if (isNameTaken(dto.getName(), parent)) {
            throw new IllegalArgumentException("Category already exists: " + dto.getName());
        }
        Category category = Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .displayOrder(dto.getDisplayOrder())
                .isActive(true)
                .tenantId(1L)
                .parent(parent)
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryDto dto) {
        Category category = getById(id);
        Category parent = resolveParent(dto.getParentId(), id);
        if (parent != null && categoryRepository.existsByParentId(id)) {
            throw new IllegalArgumentException("This category has its own subcategories and cannot become a subcategory itself");
        }
        boolean parentUnchanged = (parent == null && category.getParent() == null)
                || (parent != null && category.getParent() != null && parent.getId().equals(category.getParent().getId()));
        boolean nameUnchanged = category.getName().equalsIgnoreCase(dto.getName());
        if (!(parentUnchanged && nameUnchanged) && isNameTaken(dto.getName(), parent)) {
            throw new IllegalArgumentException("Category already exists: " + dto.getName());
        }
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setDisplayOrder(dto.getDisplayOrder());
        category.setParent(parent);
        return categoryRepository.save(category);
    }

    private Category resolveParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return null;
        }
        if (parentId.equals(selfId)) {
            throw new IllegalArgumentException("A category cannot be its own parent");
        }
        Category parent = getById(parentId);
        if (parent.getParent() != null) {
            throw new IllegalArgumentException("Cannot create a subcategory under another subcategory");
        }
        return parent;
    }

    private boolean isNameTaken(String name, Category parent) {
        return parent == null
                ? categoryRepository.existsByNameIgnoreCaseAndParentIsNull(name)
                : categoryRepository.existsByNameIgnoreCaseAndParentId(name, parent.getId());
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

    @Transactional
    public void delete(Long id) {
        Category category = getById(id);
        if (menuItemRepository.existsByCategoryId(id)) {
            throw new CategoryInUseException(id);
        }
        if (category.getParent() == null && categoryRepository.existsByParentId(id)) {
            throw new IllegalArgumentException(
                    "This category has subcategories — delete or reassign them first");
        }
        categoryRepository.delete(category);
    }
}
