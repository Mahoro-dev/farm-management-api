package rw.farm.web;

import io.swagger.v3.oas.annotations.Operation;
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
import rw.farm.dto.OperationDtos.BreedingResponse;
import rw.farm.dto.OperationDtos.CalvingResponse;
import rw.farm.dto.OperationDtos.CreateBreedingRequest;
import rw.farm.dto.OperationDtos.CreateCalvingRequest;
import rw.farm.dto.OperationDtos.PregnancyResponse;
import rw.farm.dto.OperationDtos.SavePregnancyRequest;
import rw.farm.dto.OperationDtos.UpdateBreedingRequest;
import rw.farm.service.BreedingService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Breeding")
public class BreedingController {

    private final BreedingService breeding;

    @GetMapping("/farms/{farmId}/breeding-records")
    public PageResponse<BreedingResponse> list(
            @PathVariable UUID farmId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return breeding.breeding(farmId, page, size);
    }

    @GetMapping("/animals/{animalId}/breeding-records")
    public PageResponse<BreedingResponse> forCow(
            @PathVariable UUID animalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return breeding.breedingForCow(animalId, page, size);
    }

    @Operation(description = """
            When a sire is recorded, breeding is rejected if the pair share an ancestor within the configured generations,
            including a direct parent or offspring. A veterinarian or organization administrator can set ancestryOverrideReason to record the mating anyway.
            """)
    @PostMapping("/farms/{farmId}/breeding-records")
    public ResponseEntity<BreedingResponse> create(
            @PathVariable UUID farmId, @Valid @RequestBody CreateBreedingRequest request) {
        BreedingResponse body = breeding.createBreeding(farmId, request);
        return ResponseEntity.created(URI.create("/api/breeding-records/" + body.id())).body(body);
    }

    @PutMapping("/breeding-records/{id}")
    public BreedingResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateBreedingRequest request) {
        return breeding.updateBreeding(id, request);
    }

    @GetMapping("/farms/{farmId}/pregnancies")
    public PageResponse<PregnancyResponse> pregnancies(
            @PathVariable UUID farmId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return breeding.pregnancies(farmId, page, size);
    }

    @GetMapping("/animals/{animalId}/pregnancies")
    public PageResponse<PregnancyResponse> pregnanciesForCow(
            @PathVariable UUID animalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return breeding.pregnanciesForCow(animalId, page, size);
    }

    @PostMapping("/farms/{farmId}/pregnancies")
    public ResponseEntity<PregnancyResponse> createPregnancy(
            @PathVariable UUID farmId, @Valid @RequestBody SavePregnancyRequest request) {
        PregnancyResponse body = breeding.createPregnancy(farmId, request);
        return ResponseEntity.created(URI.create("/api/pregnancies/" + body.id())).body(body);
    }

    @PutMapping("/pregnancies/{id}")
    public PregnancyResponse updatePregnancy(@PathVariable UUID id, @Valid @RequestBody SavePregnancyRequest request) {
        return breeding.updatePregnancy(id, request);
    }

    @GetMapping("/farms/{farmId}/calvings")
    public PageResponse<CalvingResponse> calvings(
            @PathVariable UUID farmId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return breeding.calvings(farmId, page, size);
    }

    @GetMapping("/animals/{animalId}/calvings")
    public PageResponse<CalvingResponse> calvingsForDam(
            @PathVariable UUID animalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return breeding.calvingsForDam(animalId, page, size);
    }

    @PostMapping("/farms/{farmId}/calvings")
    public ResponseEntity<CalvingResponse> createCalving(
            @PathVariable UUID farmId, @Valid @RequestBody CreateCalvingRequest request) {
        CalvingResponse body = breeding.createCalving(farmId, request);
        return ResponseEntity.created(URI.create("/api/calvings/" + body.id())).body(body);
    }
}
