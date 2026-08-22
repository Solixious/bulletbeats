package in.bulletbeats.domain.platform.service;

import in.bulletbeats.domain.platform.dto.OnlinePlatformDto;
import in.bulletbeats.domain.platform.entity.OnlinePlatform;
import in.bulletbeats.domain.platform.repository.OnlinePlatformRepository;
import in.bulletbeats.domain.shared.exception.DuplicatePlatformException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnlinePlatformService {

    private final OnlinePlatformRepository onlinePlatformRepository;

    public List<OnlinePlatform> listActive() {
        return onlinePlatformRepository.findByIsActiveTrueOrderByNameAsc();
    }

    public List<OnlinePlatform> listAll() {
        return onlinePlatformRepository.findAllByOrderByNameAsc();
    }

    public OnlinePlatform getById(Long id) {
        return onlinePlatformRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Online platform not found with id: " + id));
    }

    @Transactional
    public OnlinePlatform create(OnlinePlatformDto dto) {
        if (onlinePlatformRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicatePlatformException(dto.getName());
        }
        OnlinePlatform platform = OnlinePlatform.builder()
                .name(dto.getName())
                .isActive(true)
                .build();
        return onlinePlatformRepository.save(platform);
    }

    @Transactional
    public OnlinePlatform update(Long id, OnlinePlatformDto dto) {
        OnlinePlatform platform = getById(id);
        if (!platform.getName().equalsIgnoreCase(dto.getName())
                && onlinePlatformRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicatePlatformException(dto.getName());
        }
        platform.setName(dto.getName());
        return onlinePlatformRepository.save(platform);
    }

    @Transactional
    public void activate(Long id) {
        OnlinePlatform platform = getById(id);
        platform.setActive(true);
        onlinePlatformRepository.save(platform);
    }

    @Transactional
    public void deactivate(Long id) {
        OnlinePlatform platform = getById(id);
        platform.setActive(false);
        onlinePlatformRepository.save(platform);
    }
}
