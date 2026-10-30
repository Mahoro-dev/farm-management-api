package rw.farm.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.farm.common.PageResponse;
import rw.farm.domain.Enums.AnimalStatus;
import rw.farm.domain.Enums.Sex;
import rw.farm.dto.AnimalDtos.AnimalResponse;
import rw.farm.dto.AnimalDtos.ChangeStatusRequest;
import rw.farm.dto.AnimalDtos.CreateAnimalRequest;
import rw.farm.dto.AnimalDtos.FarmHistoryResponse;
import rw.farm.dto.AnimalDtos.HerdTagResponse;
import rw.farm.dto.AnimalDtos.IdentifierRequest;
import rw.farm.dto.AnimalDtos.IdentifierResponse;
import rw.farm.dto.AnimalDtos.RetireIdentifierRequest;
import rw.farm.dto.AnimalDtos.TransferRequest;
import rw.farm.dto.AnimalDtos.UpdateAnimalRequest;
import rw.farm.service.AnimalService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Animals")
public class AnimalController {

    private final AnimalService animals;

    @GetMapping("/animals")
    public PageResponse<AnimalResponse> search(
            @RequestParam(required = false) UUID farmId,
            @RequestParam(required = false) AnimalStatus status,
            @RequestParam(required = false) Sex sex,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return animals.search(farmId, status, sex, q, page, size);
    }

    @PostMapping("/animals")
    public ResponseEntity<AnimalResponse> create(@Valid @RequestBody CreateAnimalRequest request) {
        AnimalResponse body = animals.create(request);
        return ResponseEntity.created(URI.create("/api/animals/" + body.id())).body(body);
    }

    @GetMapping("/animals/lookup")
    public AnimalResponse lookup(@RequestParam String code) {
        return animals.lookup(code);
    }

    @GetMapping("/farms/{farmId}/tag-index")
    public List<HerdTagResponse> tagIndex(@PathVariable UUID farmId) {
        return animals.tagIndex(farmId);
    }

    @GetMapping("/animals/{id}")
    public AnimalResponse get(@PathVariable UUID id) {
        return animals.get(id);
    }

    @PutMapping("/animals/{id}")
    public AnimalResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAnimalRequest request) {
        return animals.update(id, request);
    }

    @PatchMapping("/animals/{id}/status")
    public AnimalResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        return animals.changeStatus(id, request);
    }

    @PostMapping("/animals/{id}/transfers")
    public AnimalResponse transfer(@PathVariable UUID id, @Valid @RequestBody TransferRequest request) {
        return animals.transfer(id, request);
    }

    @GetMapping("/animals/{id}/identifiers")
    public List<IdentifierResponse> identifiers(@PathVariable UUID id) {
        return animals.identifiers(id);
    }

    @PostMapping("/animals/{id}/identifiers")
    public ResponseEntity<IdentifierResponse> addIdentifier(
            @PathVariable UUID id, @Valid @RequestBody IdentifierRequest request) {
        IdentifierResponse body = animals.addIdentifier(id, request);
        return ResponseEntity.created(URI.create("/api/animal-identifiers/" + body.id())).body(body);
    }

    @PostMapping("/animal-identifiers/{id}/retire")
    public IdentifierResponse retire(@PathVariable UUID id, @Valid @RequestBody RetireIdentifierRequest request) {
        return animals.retireIdentifier(id, request);
    }

    @GetMapping("/animals/{id}/farm-history")
    public List<FarmHistoryResponse> farmHistory(@PathVariable UUID id) {
        return animals.farmHistory(id);
    }
}
