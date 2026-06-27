package in.bulletbeats.domain.inventory.service;

import in.bulletbeats.domain.inventory.entity.ReplenishmentRequest;
import in.bulletbeats.domain.inventory.entity.Supplier;
import in.bulletbeats.domain.inventory.repository.ReplenishmentRequestRepository;
import in.bulletbeats.domain.inventory.repository.SupplierRepository;
import in.bulletbeats.domain.shared.enums.ReplenishmentStatus;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplenishmentService {

    private final ReplenishmentRequestRepository replenishmentRequestRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final SupplierRepository supplierRepository;

    public List<ReplenishmentRequest> getPendingRequests() {
        return replenishmentRequestRepository.findByStatusWithItem(ReplenishmentStatus.PENDING);
    }

    @Transactional
    public void approveRequest(Long id, Long approverId, Long supplierId) {
        ReplenishmentRequest request = getById(id);
        request.setStatus(ReplenishmentStatus.APPROVED);
        replenishmentRequestRepository.save(request);

        Supplier supplier = resolveSupplier(supplierId, request);
        purchaseOrderService.createOrAppendPO(request, supplier);
    }

    private Supplier resolveSupplier(Long supplierId, ReplenishmentRequest request) {
        if (supplierId != null) {
            return supplierRepository.findById(supplierId).orElse(null);
        }
        return request.getGroceryItem().getDefaultSupplier();
    }

    @Transactional
    public void updateRequestedQty(Long id, BigDecimal qty) {
        ReplenishmentRequest request = getById(id);
        request.setRequestedQty(qty);
        replenishmentRequestRepository.save(request);
    }

    @Transactional
    public void cancelRequest(Long id) {
        ReplenishmentRequest request = getById(id);
        request.setStatus(ReplenishmentStatus.CANCELLED);
        replenishmentRequestRepository.save(request);
    }

    public ReplenishmentRequest getById(Long id) {
        return replenishmentRequestRepository.findByIdWithItem(id)
                .orElseThrow(() -> new ResourceNotFoundException("Replenishment request not found with id: " + id));
    }
}
