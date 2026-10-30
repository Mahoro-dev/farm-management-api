package rw.farm.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.farm.common.PageResponse;
import rw.farm.dto.OperationDtos.AuditResponse;
import rw.farm.dto.OperationDtos.CreateEventRequest;
import rw.farm.dto.OperationDtos.CreateWeightRequest;
import rw.farm.dto.OperationDtos.DailyMilkResponse;
import rw.farm.dto.OperationDtos.EventResponse;
import rw.farm.dto.OperationDtos.ExpiryWatchResponse;
import rw.farm.dto.OperationDtos.HerdAnimalResponse;
import rw.farm.dto.OperationDtos.WeightResponse;
import rw.farm.dto.OperationDtos.WithdrawalResponse;
import rw.farm.service.ReportService;
import rw.farm.service.TrackingService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Tracking and reports")
public class TrackingController {

    private final TrackingService tracking;
    private final ReportService reports;

    @GetMapping("/animals/{animalId}/weights")
    public PageResponse<WeightResponse> weights(
            @PathVariable UUID animalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tracking.weights(animalId, page, size);
    }

    @PostMapping("/animals/{animalId}/weights")
    public ResponseEntity<WeightResponse> recordWeight(
            @PathVariable UUID animalId, @Valid @RequestBody CreateWeightRequest request) {
        WeightResponse body = tracking.recordWeight(animalId, request);
        return ResponseEntity.created(URI.create("/api/weights/" + body.id())).body(body);
    }

    @GetMapping("/animals/{animalId}/events")
    public PageResponse<EventResponse> events(
            @PathVariable UUID animalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tracking.events(animalId, page, size);
    }

    @PostMapping("/animals/{animalId}/events")
    public ResponseEntity<EventResponse> recordEvent(
            @PathVariable UUID animalId, @Valid @RequestBody CreateEventRequest request) {
        EventResponse body = tracking.recordEvent(animalId, request);
        return ResponseEntity.created(URI.create("/api/events/" + body.id())).body(body);
    }

    @GetMapping("/reports/daily-milk")
    public List<DailyMilkResponse> dailyMilk(
            @RequestParam UUID farmId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return reports.dailyMilk(farmId, from, to);
    }

    @GetMapping("/reports/medicine-expiry")
    public List<ExpiryWatchResponse> medicineExpiry(
            @RequestParam UUID farmId,
            @RequestParam(defaultValue = "90") int withinDays) {
        return reports.medicineExpiry(farmId, withinDays);
    }

    @GetMapping("/reports/herd")
    public List<HerdAnimalResponse> herd(@RequestParam UUID farmId) {
        return reports.herd(farmId);
    }

    @GetMapping("/reports/withdrawals")
    public List<WithdrawalResponse> withdrawals(@RequestParam UUID farmId) {
        return reports.activeWithdrawals(farmId);
    }

    @GetMapping("/audit")
    public PageResponse<AuditResponse> audit(
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) UUID recordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reports.audit(tableName, recordId, page, size);
    }
}
