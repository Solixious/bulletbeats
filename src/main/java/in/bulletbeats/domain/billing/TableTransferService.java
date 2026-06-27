package in.bulletbeats.domain.billing;

import in.bulletbeats.domain.billing.dto.TableTransferResult;
import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.billing.service.CafeTableService;
import in.bulletbeats.domain.shared.enums.ActorType;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.enums.TableStatus;
import in.bulletbeats.domain.shared.exception.NoBillsToTransferException;
import in.bulletbeats.domain.shared.exception.TableNotActiveException;
import in.bulletbeats.domain.shared.exception.TableOccupiedException;
import in.bulletbeats.domain.user.entity.User;
import in.bulletbeats.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TableTransferService {

    private static final List<BillStatus> TRANSFERABLE = List.of(BillStatus.DRAFT, BillStatus.CONFIRMED);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final BillRepository billRepository;
    private final CafeTableService cafeTableService;
    private final UserService userService;
    private final ActivityLogService activityLogService;

    @Transactional
    public TableTransferResult transferTable(Long fromTableId, Long toTableId, Long staffUserId) {
        User staff = userService.getUserById(staffUserId);
        CafeTable fromTable = cafeTableService.getById(fromTableId);
        CafeTable toTable = cafeTableService.getById(toTableId);

        if (!toTable.isActive()) {
            throw new TableNotActiveException("Table '" + toTable.getName() + "' is not active");
        }
        if (toTable.getStatus() != TableStatus.FREE) {
            throw new TableOccupiedException("Table '" + toTable.getName() + "' is already occupied");
        }

        List<Bill> bills = billRepository.findByCafeTableIdAndStatusIn(fromTableId, TRANSFERABLE);
        if (bills.isEmpty()) {
            throw new NoBillsToTransferException(
                    "No transferable bills found on table '" + fromTable.getName() + "'");
        }

        LocalDateTime now = LocalDateTime.now();
        String t = LocalTime.now().format(TIME_FMT);
        String logMsg = "[" + t + "] Table transferred from " + fromTable.getName()
                + " to " + toTable.getName() + " by " + staff.getFullName();

        for (Bill bill : bills) {
            bill.setCafeTable(toTable);
            bill.setTransferredFromTableId(fromTableId);
            bill.setTransferredAt(now);
            billRepository.save(bill);
            activityLogService.log(bill.getId(), ActorType.STAFF, staff.getUsername(), logMsg);
        }

        cafeTableService.markOccupied(toTableId);
        cafeTableService.markFree(fromTableId);

        return new TableTransferResult(fromTable, toTable, bills.size());
    }
}
