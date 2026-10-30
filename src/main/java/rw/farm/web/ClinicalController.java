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
import rw.farm.dto.OperationDtos.AddMedicationRequest;
import rw.farm.dto.OperationDtos.CreateVaccinationRequest;
import rw.farm.dto.OperationDtos.MedicationResponse;
import rw.farm.dto.OperationDtos.SaveTreatmentRequest;
import rw.farm.dto.OperationDtos.SaveVisitRequest;
import rw.farm.dto.OperationDtos.TreatmentResponse;
import rw.farm.dto.OperationDtos.VaccinationResponse;
import rw.farm.dto.OperationDtos.VisitResponse;
import rw.farm.service.ClinicalService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Clinical")
public class ClinicalController {

    private final ClinicalService clinical;

    @GetMapping("/farms/{farmId}/veterinary-visits")
    public PageResponse<VisitResponse> visits(
            @PathVariable UUID farmId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return clinical.visits(farmId, page, size);
    }

    @GetMapping("/animals/{animalId}/veterinary-visits")
    public PageResponse<VisitResponse> visitsForAnimal(
            @PathVariable UUID animalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return clinical.visitsForAnimal(animalId, page, size);
    }

    @PostMapping("/farms/{farmId}/veterinary-visits")
    public ResponseEntity<VisitResponse> createVisit(
            @PathVariable UUID farmId, @Valid @RequestBody SaveVisitRequest request) {
        VisitResponse body = clinical.createVisit(farmId, request);
        return ResponseEntity.created(URI.create("/api/veterinary-visits/" + body.id())).body(body);
    }

    @GetMapping("/veterinary-visits/{id}")
    public VisitResponse visit(@PathVariable UUID id) {
        return clinical.visit(id);
    }

    @PutMapping("/veterinary-visits/{id}")
    public VisitResponse updateVisit(@PathVariable UUID id, @Valid @RequestBody SaveVisitRequest request) {
        return clinical.updateVisit(id, request);
    }

    @GetMapping("/farms/{farmId}/vaccinations")
    public PageResponse<VaccinationResponse> vaccinations(
            @PathVariable UUID farmId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return clinical.vaccinations(farmId, page, size);
    }

    @GetMapping("/farms/{farmId}/vaccinations/due")
    public List<VaccinationResponse> due(
            @PathVariable UUID farmId, @RequestParam(required = false) LocalDate until) {
        return clinical.due(farmId, until);
    }

    @GetMapping("/animals/{animalId}/vaccinations")
    public PageResponse<VaccinationResponse> vaccinationsForAnimal(
            @PathVariable UUID animalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return clinical.vaccinationsForAnimal(animalId, page, size);
    }

    @PostMapping("/farms/{farmId}/vaccinations")
    public ResponseEntity<VaccinationResponse> createVaccination(
            @PathVariable UUID farmId, @Valid @RequestBody CreateVaccinationRequest request) {
        VaccinationResponse body = clinical.createVaccination(farmId, request);
        return ResponseEntity.created(URI.create("/api/vaccinations/" + body.id())).body(body);
    }

    @GetMapping("/farms/{farmId}/treatments")
    public PageResponse<TreatmentResponse> treatments(
            @PathVariable UUID farmId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return clinical.treatments(farmId, page, size);
    }

    @GetMapping("/animals/{animalId}/treatments")
    public PageResponse<TreatmentResponse> treatmentsForAnimal(
            @PathVariable UUID animalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return clinical.treatmentsForAnimal(animalId, page, size);
    }

    @PostMapping("/farms/{farmId}/treatments")
    public ResponseEntity<TreatmentResponse> createTreatment(
            @PathVariable UUID farmId, @Valid @RequestBody SaveTreatmentRequest request) {
        TreatmentResponse body = clinical.createTreatment(farmId, request);
        return ResponseEntity.created(URI.create("/api/treatments/" + body.id())).body(body);
    }

    @GetMapping("/treatments/{id}")
    public TreatmentResponse treatment(@PathVariable UUID id) {
        return clinical.treatment(id);
    }

    @PutMapping("/treatments/{id}")
    public TreatmentResponse updateTreatment(@PathVariable UUID id, @Valid @RequestBody SaveTreatmentRequest request) {
        return clinical.updateTreatment(id, request);
    }

    @PostMapping("/treatments/{id}/medications")
    public ResponseEntity<MedicationResponse> addMedication(
            @PathVariable UUID id, @Valid @RequestBody AddMedicationRequest request) {
        MedicationResponse body = clinical.addMedication(id, request);
        return ResponseEntity.created(URI.create("/api/treatment-medications/" + body.id())).body(body);
    }
}
