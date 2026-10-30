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
import rw.farm.dto.AnimalDtos.BreedRequest;
import rw.farm.dto.AnimalDtos.BreedResponse;
import rw.farm.dto.AnimalDtos.SemenStrawRequest;
import rw.farm.dto.AnimalDtos.SemenStrawResponse;
import rw.farm.dto.AnimalDtos.SpeciesRequest;
import rw.farm.dto.AnimalDtos.SpeciesResponse;
import rw.farm.service.CatalogueService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Catalogue")
public class CatalogueController {

    private final CatalogueService catalogue;

    @GetMapping("/species")
    public List<SpeciesResponse> species() {
        return catalogue.species();
    }

    @PostMapping("/species")
    public ResponseEntity<SpeciesResponse> createSpecies(@Valid @RequestBody SpeciesRequest request) {
        SpeciesResponse body = catalogue.createSpecies(request);
        return ResponseEntity.created(URI.create("/api/species/" + body.id())).body(body);
    }

    @GetMapping("/breeds")
    public PageResponse<BreedResponse> breeds(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalogue.breeds(q, page, size);
    }

    @PostMapping("/breeds")
    public ResponseEntity<BreedResponse> createBreed(@Valid @RequestBody BreedRequest request) {
        BreedResponse body = catalogue.createBreed(request);
        return ResponseEntity.created(URI.create("/api/breeds/" + body.id())).body(body);
    }

    @PutMapping("/breeds/{id}")
    public BreedResponse updateBreed(@PathVariable UUID id, @Valid @RequestBody BreedRequest request) {
        return catalogue.updateBreed(id, request);
    }

    @GetMapping("/semen-straws")
    public PageResponse<SemenStrawResponse> straws(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalogue.straws(q, page, size);
    }

    @PostMapping("/semen-straws")
    public ResponseEntity<SemenStrawResponse> createStraw(@Valid @RequestBody SemenStrawRequest request) {
        SemenStrawResponse body = catalogue.createStraw(request);
        return ResponseEntity.created(URI.create("/api/semen-straws/" + body.id())).body(body);
    }

    @PutMapping("/semen-straws/{id}")
    public SemenStrawResponse updateStraw(@PathVariable UUID id, @Valid @RequestBody SemenStrawRequest request) {
        return catalogue.updateStraw(id, request);
    }
}
