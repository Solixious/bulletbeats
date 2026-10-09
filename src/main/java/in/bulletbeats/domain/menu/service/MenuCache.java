package in.bulletbeats.domain.menu.service;

import in.bulletbeats.domain.menu.dto.CategoryNode;
import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.repository.CategoryRepository;
import in.bulletbeats.domain.menu.repository.MenuItemRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * In-memory snapshot of the customer/staff-facing menu: active items (with category, dish and
 * combo already fetched), the active category tree and the promoted item. Readers always get
 * the current snapshot without touching the DB; it is swapped atomically by a background
 * thread every 5 minutes and right after any committed {@link MenuChangedEvent}.
 *
 * <p>The entities in a snapshot are detached and shared across request threads — treat them as
 * read-only. Write paths must load their own managed copy from the repository.
 */
@Slf4j
@Component
public class MenuCache {

    private static final long REFRESH_INTERVAL_MS = 5 * 60 * 1000L;

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionTemplate readOnlyTx;
    private final CacheManager cacheManager;

    private final ExecutorService refresher = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "menu-cache-refresh");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean refreshPending = new AtomicBoolean(false);
    private volatile Snapshot snapshot;

    public MenuCache(MenuItemRepository menuItemRepository,
                     CategoryRepository categoryRepository,
                     PlatformTransactionManager transactionManager,
                     CacheManager cacheManager) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.readOnlyTx = new TransactionTemplate(transactionManager);
        this.readOnlyTx.setReadOnly(true);
        this.cacheManager = cacheManager;
    }

    // ── Reads ────────────────────────────────────────────────────────────

    /** Active items ordered by category displayOrder, item displayOrder, name. */
    public List<MenuItem> activeItems() {
        return snapshot().activeItems();
    }

    /** Active and available items, same order as {@link #activeItems()}. */
    public List<MenuItem> availableItems() {
        return snapshot().availableItems();
    }

    /** Active items directly in {@code categoryId}, ordered by displayOrder, name. */
    public List<MenuItem> itemsByCategory(Long categoryId) {
        return snapshot().itemsByCategory().getOrDefault(categoryId, List.of());
    }

    /** Case-insensitive substring match on item name over active items. */
    public List<MenuItem> searchActiveItems(String q) {
        String needle = q.toLowerCase(Locale.ROOT);
        return snapshot().activeItems().stream()
                .filter(item -> item.getName().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    public Optional<MenuItem> promotedItem() {
        return snapshot().activeItems().stream().filter(MenuItem::isPromoted).findFirst();
    }

    public List<Category> activeTopLevelCategories() {
        return snapshot().topLevel();
    }

    public List<Category> activeSubcategories(Long parentId) {
        return snapshot().subcategoriesByParent().getOrDefault(parentId, List.of());
    }

    public List<CategoryNode> activeCategoryTree() {
        return snapshot().tree();
    }

    /** Active category (top-level or sub) by id, if present in the snapshot. */
    public Optional<Category> activeCategory(Long id) {
        return Optional.ofNullable(snapshot().categoriesById().get(id));
    }

    // ── Refresh triggers ─────────────────────────────────────────────────

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        requestRefresh();
    }

    @Scheduled(fixedRate = REFRESH_INTERVAL_MS, initialDelay = REFRESH_INTERVAL_MS)
    public void scheduledRefresh() {
        requestRefresh();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMenuChanged(MenuChangedEvent event) {
        Cache itemById = cacheManager.getCache("menuItems");
        if (itemById != null) {
            itemById.clear();
        }
        requestRefresh();
    }

    /**
     * Queues a background reload. Bursts of requests (e.g. several items saved in one admin
     * action) coalesce into a single reload; a request arriving mid-reload queues one more so
     * the final snapshot always reflects the latest commit.
     */
    private void requestRefresh() {
        if (refreshPending.compareAndSet(false, true)) {
            refresher.execute(() -> {
                refreshPending.set(false);
                try {
                    snapshot = load();
                } catch (RuntimeException e) {
                    log.error("Menu cache refresh failed — keeping previous snapshot", e);
                }
            });
        }
    }

    @PreDestroy
    void shutdown() {
        refresher.shutdownNow();
    }

    // ── Loading ──────────────────────────────────────────────────────────

    private Snapshot snapshot() {
        Snapshot current = snapshot;
        if (current != null) {
            return current;
        }
        // Only before the first background load has finished: load inline once.
        synchronized (this) {
            if (snapshot == null) {
                snapshot = load();
            }
            return snapshot;
        }
    }

    private Snapshot load() {
        long start = System.currentTimeMillis();
        Snapshot loaded = readOnlyTx.execute(status -> {
            List<MenuItem> active = List.copyOf(
                    menuItemRepository.findByIsActiveTrueOrderByCategoryDisplayOrderAscDisplayOrderAscNameAsc());
            List<MenuItem> available = active.stream().filter(MenuItem::isAvailable).toList();
            Map<Long, List<MenuItem>> byCategory = active.stream()
                    .collect(Collectors.groupingBy(item -> item.getCategory().getId(),
                            Collectors.toUnmodifiableList()));

            List<Category> topLevel = List.copyOf(
                    categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAscNameAsc());
            Map<Long, List<Category>> subsByParent = new HashMap<>();
            Map<Long, Category> byId = new HashMap<>();
            List<CategoryNode> tree = new ArrayList<>();
            for (Category top : topLevel) {
                List<Category> subs = List.copyOf(
                        categoryRepository.findByParentIdAndIsActiveTrueOrderByDisplayOrderAscNameAsc(top.getId()));
                subsByParent.put(top.getId(), subs);
                byId.put(top.getId(), top);
                subs.forEach(sub -> byId.put(sub.getId(), sub));
                tree.add(new CategoryNode(top, subs));
            }
            return new Snapshot(active, available, Map.copyOf(byCategory), topLevel,
                    Map.copyOf(subsByParent), List.copyOf(tree), Map.copyOf(byId));
        });
        log.debug("Menu cache loaded {} items in {} ms",
                loaded.activeItems().size(), System.currentTimeMillis() - start);
        return loaded;
    }

    private record Snapshot(
            List<MenuItem> activeItems,
            List<MenuItem> availableItems,
            Map<Long, List<MenuItem>> itemsByCategory,
            List<Category> topLevel,
            Map<Long, List<Category>> subcategoriesByParent,
            List<CategoryNode> tree,
            Map<Long, Category> categoriesById
    ) {}
}
