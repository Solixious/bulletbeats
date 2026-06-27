package in.bulletbeats.domain.billing;

import in.bulletbeats.domain.billing.dto.FloorElementDto;
import in.bulletbeats.domain.billing.dto.FloorWithTablesDto;
import in.bulletbeats.domain.billing.dto.TablePlanDto;
import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.billing.entity.Floor;
import in.bulletbeats.domain.billing.entity.FloorElement;
import in.bulletbeats.domain.billing.repository.CafeTableRepository;
import in.bulletbeats.domain.billing.repository.FloorElementRepository;
import in.bulletbeats.domain.billing.repository.FloorRepository;
import in.bulletbeats.domain.billing.service.CafeTableService;
import in.bulletbeats.domain.shared.exception.DuplicateTableException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/floor-plan")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class FloorPlanController {

    private final FloorRepository         floorRepository;
    private final CafeTableRepository     cafeTableRepository;
    private final CafeTableService        cafeTableService;
    private final FloorElementRepository  floorElementRepository;

    // ── Request records ────────────────────────────────────────────

    record CreateFloorReq(String name, Integer width, Integer height) {}

    record CreateTableReq(String name, Integer capacity, Integer seatCount,
                          Long floorId, Integer xPos, Integer yPos) {}

    record UpdateTableReq(String name, Integer capacity, Integer seatCount) {}

    record PositionReq(Integer xPos, Integer yPos, Long floorId) {}

    record DimensionsReq(Integer tableWidth, Integer tableHeight, Integer rotation) {}

    record CreateElementReq(
            String elementType, String label, Long floorId,
            Integer xPos, Integer yPos,
            Integer x2, Integer y2,
            Integer width, Integer height, Integer rotation) {}

    record ElementPositionReq(Integer xPos, Integer yPos) {}

    record UpdateElementReq(Integer rotation, Integer width, Integer height) {}

    // ── Floors ─────────────────────────────────────────────────────

    @GetMapping("/floors")
    public List<FloorWithTablesDto> getFloors() {
        return floorRepository.findAllByOrderByDisplayOrderAscNameAsc()
                .stream().map(this::toFloorDto).toList();
    }

    @PostMapping("/floors")
    @ResponseStatus(HttpStatus.CREATED)
    public FloorWithTablesDto createFloor(@RequestBody CreateFloorReq req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Floor name is required");
        }
        Floor floor = Floor.builder()
                .name(req.name().trim())
                .width(req.width()  != null && req.width()  > 0 ? req.width()  : 1200)
                .height(req.height() != null && req.height() > 0 ? req.height() : 700)
                .build();
        return toFloorDto(floorRepository.save(floor));
    }

    @PutMapping("/floors/{id}")
    public FloorWithTablesDto updateFloor(@PathVariable Long id, @RequestBody CreateFloorReq req) {
        Floor floor = floorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found: " + id));
        if (req.name()   != null && !req.name().isBlank()) floor.setName(req.name().trim());
        if (req.width()  != null && req.width()  > 0)      floor.setWidth(req.width());
        if (req.height() != null && req.height() > 0)      floor.setHeight(req.height());
        return toFloorDto(floorRepository.save(floor));
    }

    @DeleteMapping("/floors/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFloor(@PathVariable Long id) {
        long activeCount = cafeTableRepository.findActiveByFloorId(id).size();
        if (activeCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Floor has " + activeCount + " active table(s). Deactivate them first.");
        }
        floorRepository.deleteById(id);
    }

    // ── Tables ─────────────────────────────────────────────────────

    @PostMapping("/tables")
    @ResponseStatus(HttpStatus.CREATED)
    public TablePlanDto createTable(@RequestBody CreateTableReq req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table name is required");
        }
        if (req.floorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Floor is required");
        }
        try {
            CafeTable table = cafeTableService.createForFloor(
                    req.name().trim(), req.capacity(),
                    req.seatCount() != null ? req.seatCount() : 4,
                    req.floorId(),
                    req.xPos() != null ? req.xPos() : 100,
                    req.yPos() != null ? req.yPos() : 100);
            return toTableDto(table);
        } catch (DuplicateTableException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PatchMapping("/tables/{id}/position")
    public TablePlanDto updatePosition(@PathVariable Long id, @RequestBody PositionReq req) {
        if (req.xPos() == null || req.yPos() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "xPos and yPos are required");
        }
        return toTableDto(cafeTableService.updatePosition(id, req.floorId(), req.xPos(), req.yPos()));
    }

    @PatchMapping("/tables/{id}")
    public TablePlanDto updateTable(@PathVariable Long id, @RequestBody UpdateTableReq req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table name is required");
        }
        try {
            CafeTable table = cafeTableService.updatePlanDetails(
                    id, req.name().trim(), req.capacity(),
                    req.seatCount() != null ? req.seatCount() : 4);
            return toTableDto(table);
        } catch (DuplicateTableException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PatchMapping("/tables/{id}/dimensions")
    public TablePlanDto updateDimensions(@PathVariable Long id, @RequestBody DimensionsReq req) {
        int w = req.tableWidth()  != null ? req.tableWidth()  : 100;
        int h = req.tableHeight() != null ? req.tableHeight() : 68;
        int r = req.rotation()    != null ? req.rotation()    : 0;
        return toTableDto(cafeTableService.updateDimensions(id, w, h, r));
    }

    @PostMapping("/tables/{id}/deactivate")
    public ResponseEntity<?> deactivateTable(@PathVariable Long id) {
        try {
            cafeTableService.deactivate(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Floor elements ─────────────────────────────────────────────

    @PostMapping("/elements")
    @ResponseStatus(HttpStatus.CREATED)
    public FloorElementDto createElement(@RequestBody CreateElementReq req) {
        if (req.floorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Floor is required");
        }
        if (!floorRepository.existsById(req.floorId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Floor not found");
        }
        String type = req.elementType() != null ? req.elementType() : "CASH_COUNTER";

        // Enforce cash-counter singleton per floor
        if ("CASH_COUNTER".equals(type)
                && floorElementRepository.countByFloorIdAndElementType(req.floorId(), "CASH_COUNTER") > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only one cash counter is allowed per floor");
        }

        int defaultWidth  = "CASH_COUNTER".equals(type) ? 72 : 60;
        int defaultHeight = "CASH_COUNTER".equals(type) ? 40 : 0;
        FloorElement el = FloorElement.builder()
                .floorId(req.floorId())
                .elementType(type)
                .label(req.label()       != null ? req.label()       : labelFor(type))
                .xPos(req.xPos()         != null ? req.xPos()        : 60)
                .yPos(req.yPos()         != null ? req.yPos()        : 60)
                .x2(req.x2())
                .y2(req.y2())
                .width(req.width()       != null ? req.width()       : defaultWidth)
                .height(req.height()     != null ? req.height()      : ("CASH_COUNTER".equals(type) ? defaultHeight : null))
                .rotation(req.rotation() != null ? req.rotation()    : 0)
                .build();
        return toElementDto(floorElementRepository.save(el));
    }

    @PatchMapping("/elements/{id}/position")
    public FloorElementDto updateElementPosition(@PathVariable Long id, @RequestBody ElementPositionReq req) {
        FloorElement el = findElement(id);
        if (req.xPos() != null) el.setXPos(req.xPos());
        if (req.yPos() != null) el.setYPos(req.yPos());
        return toElementDto(floorElementRepository.save(el));
    }

    @PatchMapping("/elements/{id}")
    public FloorElementDto updateElement(@PathVariable Long id, @RequestBody UpdateElementReq req) {
        FloorElement el = findElement(id);
        if (req.rotation() != null) el.setRotation(((req.rotation() % 360) + 360) % 360);
        if (req.width()    != null && req.width()  > 0) el.setWidth(Math.max(40, Math.min(300, req.width())));
        if (req.height()   != null && req.height() > 0) el.setHeight(Math.max(24, Math.min(200, req.height())));
        return toElementDto(floorElementRepository.save(el));
    }

    @DeleteMapping("/elements/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteElement(@PathVariable Long id) {
        if (!floorElementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Element not found: " + id);
        }
        floorElementRepository.deleteById(id);
    }

    // ── Helpers ────────────────────────────────────────────────────

    private FloorElement findElement(Long id) {
        return floorElementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Element not found: " + id));
    }

    private static String labelFor(String type) {
        return switch (type) {
            case "WALL" -> "Wall";
            case "DOOR" -> "Door";
            default     -> "CASH";
        };
    }

    private FloorWithTablesDto toFloorDto(Floor f) {
        List<TablePlanDto>    tables   = cafeTableRepository.findActiveByFloorId(f.getId())
                .stream().map(this::toTableDto).toList();
        List<FloorElementDto> elements = floorElementRepository.findByFloorId(f.getId())
                .stream().map(this::toElementDto).toList();
        return new FloorWithTablesDto(f.getId(), f.getName(), f.getDisplayOrder(),
                f.getWidth(), f.getHeight(), tables, elements);
    }

    private TablePlanDto toTableDto(CafeTable t) {
        return new TablePlanDto(
                t.getId(), t.getName(), t.getCapacity(), t.getSeatCount(),
                t.getXPos(), t.getYPos(), t.getStatus().name(),
                t.isActive(), t.getFloor() != null ? t.getFloor().getId() : null,
                t.getTableWidth(), t.getTableHeight(), t.getRotation());
    }

    private FloorElementDto toElementDto(FloorElement el) {
        return new FloorElementDto(
                el.getId(), el.getElementType(), el.getLabel(),
                el.getXPos(), el.getYPos(), el.getFloorId(),
                el.getX2(), el.getY2(), el.getWidth(), el.getHeight(), el.getRotation());
    }
}
