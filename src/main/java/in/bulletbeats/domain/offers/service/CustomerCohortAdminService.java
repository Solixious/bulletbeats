package in.bulletbeats.domain.offers.service;

import in.bulletbeats.domain.offers.dto.CustomerCohortDto;
import in.bulletbeats.domain.offers.entity.CustomerCohort;
import in.bulletbeats.domain.offers.entity.CustomerCohortRule;
import in.bulletbeats.domain.offers.repository.CustomerCohortRepository;
import in.bulletbeats.domain.offers.repository.OfferRepository;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerCohortAdminService {

    private final CustomerCohortRepository cohortRepository;
    private final OfferRepository offerRepository;

    public List<CustomerCohort> getAll() {
        return cohortRepository.findAllByOrderByNameAsc();
    }

    public CustomerCohort getById(Long id) {
        return cohortRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found with id: " + id));
    }

    @Transactional
    public CustomerCohort create(CustomerCohortDto dto) {
        CustomerCohort cohort = CustomerCohort.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .builtIn(false)
                .active(true)
                .build();
        applyRules(cohort, dto);
        return cohortRepository.save(cohort);
    }

    @Transactional
    public CustomerCohort update(Long id, CustomerCohortDto dto) {
        CustomerCohort cohort = getById(id);
        cohort.setName(dto.getName());
        cohort.setDescription(dto.getDescription());
        if (!cohort.isBuiltIn()) {
            applyRules(cohort, dto);
        }
        return cohortRepository.save(cohort);
    }

    private void applyRules(CustomerCohort cohort, CustomerCohortDto dto) {
        cohort.getRules().clear();
        if (dto.getRules() == null) {
            return;
        }
        List<CustomerCohortRule> rules = new ArrayList<>();
        for (var ruleDto : dto.getRules()) {
            if (ruleDto.getField() == null || ruleDto.getOperator() == null || ruleDto.getValue() == null) {
                continue;
            }
            rules.add(CustomerCohortRule.builder()
                    .cohort(cohort)
                    .field(ruleDto.getField())
                    .operator(ruleDto.getOperator())
                    .value(ruleDto.getValue())
                    .periodDays(ruleDto.getPeriodDays())
                    .build());
        }
        cohort.getRules().addAll(rules);
    }

    @Transactional
    public void delete(Long id) {
        CustomerCohort cohort = getById(id);
        if (cohort.isBuiltIn()) {
            throw new IllegalArgumentException("Built-in cohorts cannot be deleted");
        }
        if (offerRepository.existsByCohortId(id)) {
            throw new IllegalArgumentException("This cohort is used by one or more offers and cannot be deleted");
        }
        cohortRepository.delete(cohort);
    }
}
