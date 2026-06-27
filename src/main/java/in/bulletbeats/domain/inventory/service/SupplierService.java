package in.bulletbeats.domain.inventory.service;

import in.bulletbeats.domain.inventory.dto.SupplierDto;
import in.bulletbeats.domain.inventory.entity.Supplier;
import in.bulletbeats.domain.inventory.repository.SupplierRepository;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import in.bulletbeats.domain.shared.exception.SupplierAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public List<Supplier> getAllActiveSuppliers() {
        return supplierRepository.findByIsActiveTrueOrderByNameAsc();
    }

    public List<Supplier> getAllInactiveSuppliers() {
        return supplierRepository.findByIsActiveFalseOrderByNameAsc();
    }

    public Supplier getById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    @Transactional
    public Supplier create(SupplierDto dto) {
        if (supplierRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new SupplierAlreadyExistsException(dto.getName());
        }
        Supplier supplier = Supplier.builder()
                .name(dto.getName())
                .phone(dto.getPhone())
                .isActive(true)
                .tenantId(1L)
                .build();
        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier update(Long id, SupplierDto dto) {
        Supplier supplier = getById(id);
        supplier.setName(dto.getName());
        supplier.setPhone(dto.getPhone());
        return supplierRepository.save(supplier);
    }

    @Transactional
    public void deactivate(Long id) {
        Supplier supplier = getById(id);
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    @Transactional
    public void reactivate(Long id) {
        Supplier supplier = getById(id);
        supplier.setActive(true);
        supplierRepository.save(supplier);
    }
}
