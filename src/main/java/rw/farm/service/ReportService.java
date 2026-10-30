package rw.farm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.farm.common.ApiException;
import rw.farm.common.PageResponse;
import rw.farm.domain.AuditLog;
import rw.farm.domain.Enums.AnimalStatus;
import rw.farm.domain.Enums.Sex;
import rw.farm.domain.Enums.TreatmentOutcome;
import rw.farm.dto.OperationDtos.AuditResponse;
import rw.farm.dto.OperationDtos.DailyMilkResponse;
import rw.farm.dto.OperationDtos.ExpiryWatchResponse;
import rw.farm.dto.OperationDtos.HerdAnimalResponse;
import rw.farm.dto.OperationDtos.WithdrawalResponse;
import rw.farm.repo.AuditLogRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final JdbcTemplate jdbc;
    private final AuditLogRepository auditLog;
    private final AccessService access;

    public List<DailyMilkResponse> dailyMilk(UUID farmId, LocalDate from, LocalDate to) {
        access.requireFarmRead(farmId);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        if (start.isAfter(end)) {
            throw ApiException.badRequest("from cannot be after to");
        }
        return jdbc.query("""
                SELECT farm_id, milking_date, total_liters, animals_milked
                FROM v_daily_milk_totals
                WHERE farm_id = ? AND milking_date BETWEEN ? AND ?
                ORDER BY milking_date DESC
                """, (rs, row) -> new DailyMilkResponse(
                UUID.fromString(rs.getString("farm_id")),
                rs.getObject("milking_date", LocalDate.class),
                rs.getBigDecimal("total_liters"),
                rs.getLong("animals_milked")), farmId.toString(), start, end);
    }

    public List<ExpiryWatchResponse> medicineExpiry(UUID farmId, int withinDays) {
        access.requireFarmRead(farmId);
        if (withinDays < 0 || withinDays > 3650) {
            throw ApiException.badRequest("withinDays must be between 0 and 3650");
        }
        return jdbc.query("""
                SELECT mb.id AS batch_id, mb.farm_id, mp.name AS medicine_name,
                       mb.batch_number, mb.expiry_date, mb.quantity_on_hand, mb.unit_cost
                FROM medicine_batches mb
                JOIN medicine_products mp ON mp.id = mb.medicine_product_id
                WHERE mb.farm_id = ?
                  AND mb.quantity_on_hand > 0
                  AND mb.expiry_date <= DATE_ADD(CURRENT_DATE, INTERVAL ? DAY)
                ORDER BY mb.expiry_date
                """, (rs, row) -> new ExpiryWatchResponse(
                UUID.fromString(rs.getString("batch_id")),
                UUID.fromString(rs.getString("farm_id")),
                rs.getString("medicine_name"),
                rs.getString("batch_number"),
                rs.getObject("expiry_date", LocalDate.class),
                rs.getBigDecimal("quantity_on_hand"),
                rs.getBigDecimal("unit_cost")), farmId.toString(), withinDays);
    }

    public List<HerdAnimalResponse> herd(UUID farmId) {
        access.requireFarmRead(farmId);
        return jdbc.query("""
                SELECT a.id, a.animal_number, a.name, a.sex, a.status, h.joined_on, b.name AS breed_name
                FROM animal_farm_history h
                JOIN animals a ON a.id = h.animal_id
                LEFT JOIN cattle_breeds b ON b.id = a.breed_id
                WHERE h.farm_id = ? AND h.left_on IS NULL
                ORDER BY a.animal_number
                """, (rs, row) -> new HerdAnimalResponse(
                UUID.fromString(rs.getString("id")),
                rs.getString("animal_number"),
                rs.getString("name"),
                Sex.valueOf(rs.getString("sex")),
                AnimalStatus.valueOf(rs.getString("status")),
                rs.getObject("joined_on", LocalDate.class),
                rs.getString("breed_name")), farmId.toString());
    }

    public List<WithdrawalResponse> activeWithdrawals(UUID farmId) {
        access.requireFarmRead(farmId);
        return jdbc.query("""
                SELECT t.id, t.animal_id, a.animal_number, t.milk_withdrawal_until,
                       t.meat_withdrawal_until, t.outcome
                FROM treatments t
                JOIN animals a ON a.id = t.animal_id
                WHERE t.farm_id = ?
                  AND (t.milk_withdrawal_until > UTC_TIMESTAMP(6) OR t.meat_withdrawal_until > UTC_TIMESTAMP(6))
                ORDER BY t.milk_withdrawal_until
                """, (rs, row) -> new WithdrawalResponse(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("animal_id")),
                rs.getString("animal_number"),
                instant(rs.getTimestamp("milk_withdrawal_until")),
                instant(rs.getTimestamp("meat_withdrawal_until")),
                TreatmentOutcome.valueOf(rs.getString("outcome"))), farmId.toString());
    }

    public PageResponse<AuditResponse> audit(String tableName, UUID recordId, int page, int size) {
        access.requireOrgAdmin();
        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "changedAt"));
        return PageResponse.of(auditLog.search(blank(tableName), recordId, pageable).map(this::toResponse));
    }

    private AuditResponse toResponse(AuditLog entry) {
        return new AuditResponse(
                entry.getId(),
                entry.getTableName(),
                entry.getRecordId(),
                entry.getAction(),
                entry.getChangedAt(),
                entry.getChangedBy(),
                entry.getOldData(),
                entry.getNewData());
    }

    private Instant instant(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().toInstant(ZoneOffset.UTC);
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
