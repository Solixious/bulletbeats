package in.bulletbeats.domain.waiting.service;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import in.bulletbeats.domain.waiting.dto.WaitingActivityDto;
import in.bulletbeats.domain.waiting.dto.WaitingActivityGroup;
import in.bulletbeats.domain.waiting.entity.WaitingActivity;
import in.bulletbeats.domain.waiting.entity.WaitingActivityCategory;
import in.bulletbeats.domain.waiting.repository.WaitingActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WaitingActivityService {

    private static final String KEY_ENABLED = "waiting.enabled";
    private static final String KEY_TEASER_MESSAGE = "waiting.teaser_message";
    private static final String KEY_HELP_MESSAGE = "waiting.help_message";

    private final WaitingActivityRepository waitingActivityRepository;
    private final AppConfigService appConfigService;

    // ── Admin ────────────────────────────────────────────────────────────

    public List<WaitingActivity> getAll() {
        return waitingActivityRepository.findAllByOrderByCategoryAscSortOrderAscNameAsc();
    }

    @Transactional
    public WaitingActivity create(WaitingActivityDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (dto.getCategory() == null) {
            throw new IllegalArgumentException("Category is required");
        }
        return waitingActivityRepository.save(WaitingActivity.builder()
                .category(dto.getCategory())
                .name(dto.getName().trim())
                .sortOrder(dto.getSortOrder())
                .isActive(true)
                .build());
    }

    @Transactional
    public void toggleActive(Long id) {
        WaitingActivity item = getById(id);
        item.setActive(!item.isActive());
    }

    @Transactional
    public void delete(Long id) {
        waitingActivityRepository.delete(getById(id));
    }

    private WaitingActivity getById(Long id) {
        return waitingActivityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Waiting activity not found with id: " + id));
    }

    // ── Customer-facing ─────────────────────────────────────────────────

    /** Whether the "while you wait" widget should be shown at all: enabled and at least one active item. */
    public boolean isEnabled() {
        return appConfigService.getBoolean(KEY_ENABLED, true) && waitingActivityRepository.existsByIsActiveTrue();
    }

    public String getTeaserMessage() {
        return appConfigService.get(KEY_TEASER_MESSAGE, "Bored while you wait? We've got games to keep you entertained!");
    }

    public String getHelpMessage() {
        return appConfigService.get(KEY_HELP_MESSAGE, "Can't find something on the list? Just ask a staff member — we're happy to help!");
    }

    /** Active items grouped by category, in display order, for the "see more" modal. */
    public List<WaitingActivityGroup> getActiveGrouped() {
        List<WaitingActivity> items = waitingActivityRepository.findAllByIsActiveTrueOrderByCategoryAscSortOrderAscNameAsc();
        Map<WaitingActivityCategory, List<WaitingActivity>> byCategory = new LinkedHashMap<>();
        for (WaitingActivity item : items) {
            byCategory.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
        }
        return byCategory.entrySet().stream()
                .map(e -> new WaitingActivityGroup(e.getKey(), e.getValue()))
                .toList();
    }
}
