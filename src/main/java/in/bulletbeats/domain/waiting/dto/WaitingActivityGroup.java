package in.bulletbeats.domain.waiting.dto;

import in.bulletbeats.domain.waiting.entity.WaitingActivity;
import in.bulletbeats.domain.waiting.entity.WaitingActivityCategory;

import java.util.List;

public record WaitingActivityGroup(WaitingActivityCategory category, List<WaitingActivity> items) {
}
