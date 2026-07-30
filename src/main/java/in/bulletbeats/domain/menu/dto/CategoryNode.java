package in.bulletbeats.domain.menu.dto;

import in.bulletbeats.domain.menu.entity.Category;

import java.util.List;

public record CategoryNode(
        Category category,
        List<Category> subcategories
) {}
