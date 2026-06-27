package in.bulletbeats.domain.billing.dto;

import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.entity.MenuItem;

import java.util.List;

public record CategoryWithItemsDto(
        Category category,
        List<MenuItem> items
) {}
