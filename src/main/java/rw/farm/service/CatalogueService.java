package rw.farm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.farm.common.ApiException;
import rw.farm.common.PageResponse;
import rw.farm.common.Pages;
import rw.farm.domain.AnimalSpecies;
import rw.farm.domain.CattleBreed;
import rw.farm.domain.Enums.RoleCode;
import rw.farm.domain.SemenStraw;
import rw.farm.dto.AnimalDtos.BreedRequest;
import rw.farm.dto.AnimalDtos.BreedResponse;
import rw.farm.dto.AnimalDtos.SemenStrawRequest;
import rw.farm.dto.AnimalDtos.SemenStrawResponse;
import rw.farm.dto.AnimalDtos.SpeciesRequest;
import rw.farm.dto.AnimalDtos.SpeciesResponse;
import rw.farm.repo.AnimalSpeciesRepository;
import rw.farm.repo.CattleBreedRepository;
import rw.farm.repo.SemenStrawRepository;
import rw.farm.security.SecurityUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogueService {

    private final AnimalSpeciesRepository species;
    private final CattleBreedRepository breeds;
    private final SemenStrawRepository straws;
    private final AccessService access;

    public List<SpeciesResponse> species() {
        access.requireMember();
        return species.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public SpeciesResponse createSpecies(SpeciesRequest request) {
        access.requireOrgAdmin();
        String code = request.code().trim().toUpperCase();
        species.findByCodeIgnoreCase(code).ifPresent(existing -> {
            throw ApiException.conflict("A species with this code already exists");
        });
        AnimalSpecies created = new AnimalSpecies();
        created.setCode(code);
        created.setName(request.name().trim());
        created.setActive(true);
        return toResponse(species.save(created));
    }

    public PageResponse<BreedResponse> breeds(String q, int page, int size) {
        access.requireMember();
        var pageable = Pages.of(page, size);
        var result = (q == null || q.isBlank())
                ? breeds.findAll(pageable)
                : breeds.findByNameContainingIgnoreCase(q.trim(), pageable);
        return PageResponse.of(result.map(this::toResponse));
    }

    @Transactional
    public BreedResponse createBreed(BreedRequest request) {
        access.requireAnyRole(RoleCode.FARM_MANAGER);
        CattleBreed breed = new CattleBreed();
        apply(breed, request);
        return toResponse(breeds.save(breed));
    }

    @Transactional
    public BreedResponse updateBreed(UUID id, BreedRequest request) {
        access.requireAnyRole(RoleCode.FARM_MANAGER);
        CattleBreed breed = breeds.findById(id).orElseThrow(() -> ApiException.notFound("Breed not found"));
        apply(breed, request);
        return toResponse(breed);
    }

    private void apply(CattleBreed breed, BreedRequest request) {
        breed.setName(request.name().trim());
        breed.setDescription(DomainSupport.trimToNull(request.description()));
        if (request.active() != null) {
            breed.setActive(request.active());
        }
    }

    private SpeciesResponse toResponse(AnimalSpecies item) {
        return new SpeciesResponse(item.getId(), item.getCode(), item.getName(), item.isActive());
    }

    private BreedResponse toResponse(CattleBreed item) {
        return new BreedResponse(item.getId(), item.getName(), item.getDescription(), item.isActive());
    }

    public PageResponse<SemenStrawResponse> straws(String q, int page, int size) {
        access.requireMember();
        UUID organizationId = SecurityUtils.organizationId();
        var pageable = Pages.of(page, size);
        var result = (q == null || q.isBlank())
                ? straws.findByOrganizationId(organizationId, pageable)
                : straws.findByOrganizationIdAndCodeContainingIgnoreCase(organizationId, q.trim(), pageable);
        return PageResponse.of(result.map(this::toResponse));
    }

    @Transactional
    public SemenStrawResponse createStraw(SemenStrawRequest request) {
        access.requireAnyRole(RoleCode.FARM_MANAGER, RoleCode.VETERINARIAN);
        SemenStraw straw = new SemenStraw();
        straw.setOrganization(access.currentOrganization());
        apply(straw, request);
        return toResponse(straws.save(straw));
    }

    @Transactional
    public SemenStrawResponse updateStraw(UUID id, SemenStrawRequest request) {
        access.requireAnyRole(RoleCode.FARM_MANAGER, RoleCode.VETERINARIAN);
        SemenStraw straw = loadStraw(id);
        apply(straw, request);
        return toResponse(straw);
    }

    private void apply(SemenStraw straw, SemenStrawRequest request) {
        String code = request.code().trim();
        straws.findByOrganizationIdAndCodeIgnoreCase(straw.getOrganization().getId(), code)
                .filter(existing -> straw.getId() == null || !existing.getId().equals(straw.getId()))
                .ifPresent(existing -> {
                    throw ApiException.conflict("A straw with this code already exists");
                });
        straw.setCode(code);
        straw.setDescription(DomainSupport.trimToNull(request.description()));
        if (request.active() != null) {
            straw.setActive(request.active());
        }
    }

    private SemenStraw loadStraw(UUID id) {
        SemenStraw straw = straws.findById(id).orElseThrow(() -> ApiException.notFound("Straw not found"));
        if (!straw.getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Straw not found");
        }
        return straw;
    }

    private SemenStrawResponse toResponse(SemenStraw item) {
        return new SemenStrawResponse(item.getId(), item.getCode(), item.getDescription(), item.isActive());
    }
}
