package in.bulletbeats.domain.billing.service;

import in.bulletbeats.domain.billing.dto.CafeTableDto;
import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.billing.entity.Floor;
import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.billing.repository.CafeTableRepository;
import in.bulletbeats.domain.billing.repository.FloorRepository;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.enums.TableStatus;
import in.bulletbeats.domain.shared.exception.DuplicateTableException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CafeTableService {

    private final CafeTableRepository cafeTableRepository;
    private final BillRepository billRepository;
    private final QrCodeService qrCodeService;
    private final JdbcTemplate jdbcTemplate;
    private final FloorRepository floorRepository;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public List<CafeTable> getAllActive() {
        return cafeTableRepository.findByIsActiveTrueOrderByNameAsc();
    }

    public CafeTable getById(Long id) {
        return cafeTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + id));
    }

    public CafeTable getByQrCode(String code) {
        return cafeTableRepository.findByQrCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found for QR code: " + code));
    }

    public byte[] getQrBytes(Long id) {
        CafeTable table = getById(id);
        if (table.getQrImagePath() == null) {
            throw new ResourceNotFoundException("No QR code generated for table " + id);
        }
        try {
            return Files.readAllBytes(Paths.get(uploadDir, table.getQrImagePath()));
        } catch (IOException e) {
            throw new ResourceNotFoundException("QR image not found for table " + id);
        }
    }

    @Transactional
    public CafeTable create(CafeTableDto dto) {
        if (cafeTableRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateTableException(dto.getName());
        }
        String qrCode = UUID.randomUUID().toString();
        CafeTable table = CafeTable.builder()
                .name(dto.getName())
                .capacity(dto.getCapacity())
                .qrCode(qrCode)
                .build();
        CafeTable saved = cafeTableRepository.save(table);

        String content = readBaseUrl() + "/qr/" + qrCode;
        saved.setQrImagePath(qrCodeService.generateAndSave(content, saved.getId() + ".png"));
        return cafeTableRepository.save(saved);
    }

    @Transactional
    public CafeTable update(Long id, CafeTableDto dto) {
        CafeTable table = getById(id);
        if (!table.getName().equalsIgnoreCase(dto.getName())
                && cafeTableRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateTableException(dto.getName());
        }
        table.setName(dto.getName());
        table.setCapacity(dto.getCapacity());
        return cafeTableRepository.save(table);
    }

    @Transactional
    public void deactivate(Long id) {
        CafeTable table = getById(id);
        List<BillStatus> terminal = List.of(BillStatus.PAID, BillStatus.CANCELLED);
        long activeBillCount = billRepository.countByCafeTableIdAndStatusNotIn(id, terminal);
        if (activeBillCount > 0) {
            throw new IllegalStateException(
                    "Table '" + table.getName() + "' has " + activeBillCount + " active bill(s)");
        }
        table.setActive(false);
        cafeTableRepository.save(table);
    }

    @Transactional
    public void markOccupied(Long tableId) {
        CafeTable table = getById(tableId);
        table.setStatus(TableStatus.OCCUPIED);
        cafeTableRepository.save(table);
    }

    @Transactional
    public void markFree(Long tableId) {
        CafeTable table = getById(tableId);
        table.setStatus(TableStatus.FREE);
        cafeTableRepository.save(table);
    }

    @Transactional
    public CafeTable createForFloor(String name, Integer capacity, int seatCount,
                                    Long floorId, int xPos, int yPos) {
        if (cafeTableRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateTableException(name);
        }
        Floor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found: " + floorId));
        String qrCode = UUID.randomUUID().toString();
        CafeTable table = CafeTable.builder()
                .name(name)
                .capacity(capacity)
                .qrCode(qrCode)
                .floor(floor)
                .xPos(xPos)
                .yPos(yPos)
                .seatCount(seatCount)
                .build();
        CafeTable saved = cafeTableRepository.save(table);
        String content = readBaseUrl() + "/qr/" + qrCode;
        saved.setQrImagePath(qrCodeService.generateAndSave(content, saved.getId() + ".png"));
        return cafeTableRepository.save(saved);
    }

    @Transactional
    public CafeTable updatePosition(Long id, Long floorId, int xPos, int yPos) {
        CafeTable table = getById(id);
        if (floorId != null && (table.getFloor() == null || !table.getFloor().getId().equals(floorId))) {
            Floor floor = floorRepository.findById(floorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Floor not found: " + floorId));
            table.setFloor(floor);
        }
        table.setXPos(xPos);
        table.setYPos(yPos);
        return cafeTableRepository.save(table);
    }

    @Transactional
    public CafeTable updateDimensions(Long id, int width, int height, int rotation) {
        CafeTable table = getById(id);
        table.setTableWidth(Math.max(60, Math.min(400, width)));
        table.setTableHeight(Math.max(40, Math.min(300, height)));
        table.setRotation(((rotation % 360) + 360) % 360);
        return cafeTableRepository.save(table);
    }

    @Transactional
    public CafeTable updatePlanDetails(Long id, String name, Integer capacity, int seatCount) {
        CafeTable table = getById(id);
        if (!table.getName().equalsIgnoreCase(name) && cafeTableRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateTableException(name);
        }
        table.setName(name);
        table.setCapacity(capacity);
        table.setSeatCount(seatCount);
        return cafeTableRepository.save(table);
    }

    @Transactional
    public CafeTable regenerateQrCode(Long id) {
        CafeTable table = getById(id);
        if (table.getQrImagePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(uploadDir, table.getQrImagePath()));
            } catch (IOException ignored) {
            }
        }
        String newQrCode = UUID.randomUUID().toString();
        String content = readBaseUrl() + "/qr/" + newQrCode;
        table.setQrCode(newQrCode);
        table.setQrImagePath(qrCodeService.generateAndSave(content, id + ".png"));
        return cafeTableRepository.save(table);
    }

    private String readBaseUrl() {
        try {
            String value = jdbcTemplate.queryForObject(
                    "SELECT value FROM app_config WHERE key = 'app.base-url'", String.class);
            return value != null ? value : "http://localhost:8080";
        } catch (EmptyResultDataAccessException e) {
            return "http://localhost:8080";
        }
    }
}
