package rw.farm.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.farm.common.PageResponse;
import rw.farm.dto.OperationDtos.LactationStageResponse;
import rw.farm.dto.OperationDtos.MilkResponse;
import rw.farm.dto.OperationDtos.SaveMilkRequest;
import rw.farm.dto.OperationDtos.SaveShiftRequest;
import rw.farm.dto.OperationDtos.ShiftResponse;
import rw.farm.service.MilkService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Milk")
public class MilkController {

    private final MilkService milk;

    @GetMapping("/lactation-stages")
    public List<LactationStageResponse> stages() {
        return milk.stages();
    }

    @GetMapping("/farms/{farmId}/shifts")
    public List<ShiftResponse> shifts(@PathVariable UUID farmId) {
        return milk.shifts(farmId);
    }

    @PostMapping("/farms/{farmId}/shifts")
    public ResponseEntity<ShiftResponse> createShift(
            @PathVariable UUID farmId, @Valid @RequestBody SaveShiftRequest request) {
        ShiftResponse body = milk.createShift(farmId, request);
        return ResponseEntity.created(URI.create("/api/shifts/" + body.id())).body(body);
    }

    @PutMapping("/shifts/{id}")
    public ShiftResponse updateShift(@PathVariable UUID id, @Valid @RequestBody SaveShiftRequest request) {
        return milk.updateShift(id, request);
    }

    @GetMapping("/farms/{farmId}/milk-records")
    public PageResponse<MilkResponse> records(
            @PathVariable UUID farmId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return milk.records(farmId, from, to, page, size);
    }

    @GetMapping("/animals/{animalId}/milk-records")
    public PageResponse<MilkResponse> recordsForAnimal(
            @PathVariable UUID animalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return milk.recordsForAnimal(animalId, page, size);
    }

    @PostMapping("/farms/{farmId}/milk-records")
    public ResponseEntity<MilkResponse> createRecord(
            @PathVariable UUID farmId, @Valid @RequestBody SaveMilkRequest request) {
        MilkResponse body = milk.createRecord(farmId, request);
        return ResponseEntity.created(URI.create("/api/milk-records/" + body.id())).body(body);
    }

    @PutMapping("/milk-records/{id}")
    public MilkResponse updateRecord(@PathVariable UUID id, @Valid @RequestBody SaveMilkRequest request) {
        return milk.updateRecord(id, request);
    }
}
