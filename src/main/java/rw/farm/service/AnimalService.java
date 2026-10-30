package rw.farm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.farm.common.ApiException;
import rw.farm.common.PageResponse;
import rw.farm.common.Pages;
import rw.farm.domain.Animal;
import rw.farm.domain.AnimalEvent;
import rw.farm.domain.AnimalFarmHistory;
import rw.farm.domain.AnimalIdentifier;
import rw.farm.domain.AnimalSpecies;
import rw.farm.domain.CattleBreed;
import rw.farm.domain.Enums.AcquisitionType;
import rw.farm.domain.Enums.AnimalEventType;
import rw.farm.domain.Enums.AnimalStatus;
import rw.farm.domain.Enums.Sex;
import rw.farm.domain.Enums.TransferReason;
import rw.farm.domain.Farm;
import rw.farm.domain.SemenStraw;
import rw.farm.dto.AnimalDtos.AnimalResponse;
import rw.farm.dto.AnimalDtos.ChangeStatusRequest;
import rw.farm.dto.AnimalDtos.CreateAnimalRequest;
import rw.farm.dto.AnimalDtos.FarmHistoryResponse;
import rw.farm.dto.AnimalDtos.HerdTagResponse;
import rw.farm.dto.AnimalDtos.IdentifierRequest;
import rw.farm.dto.AnimalDtos.IdentifierResponse;
import rw.farm.dto.AnimalDtos.RetireIdentifierRequest;
import rw.farm.dto.AnimalDtos.TagResponse;
import rw.farm.dto.AnimalDtos.TransferRequest;
import rw.farm.dto.AnimalDtos.UpdateAnimalRequest;
import rw.farm.repo.AnimalEventRepository;
import rw.farm.repo.AnimalFarmHistoryRepository;
import rw.farm.repo.AnimalIdentifierRepository;
import rw.farm.repo.AnimalRepository;
import rw.farm.repo.AnimalSpeciesRepository;
import rw.farm.repo.CattleBreedRepository;
import rw.farm.repo.SemenStrawRepository;
import rw.farm.security.SecurityUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static rw.farm.service.AccessService.ANIMAL_WRITE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnimalService {

    private final AnimalRepository animals;
    private final AnimalSpeciesRepository speciesRepo;
    private final CattleBreedRepository breeds;
    private final SemenStrawRepository straws;
    private final AnimalFarmHistoryRepository history;
    private final AnimalIdentifierRepository identifiers;
    private final AnimalEventRepository events;
    private final AccessService access;
    private final DomainSupport support;

    public PageResponse<AnimalResponse> search(
            UUID farmId, AnimalStatus status, Sex sex, String q, int page, int size) {
        boolean restrict = !access.isOrgAdmin();
        List<UUID> farmIds = restrict ? access.accessibleFarmIds() : List.of(DomainSupport.placeholderFarm());
        if (farmId != null) {
            access.requireFarmRead(farmId);
        } else if (restrict && farmIds.isEmpty()) {
            return new PageResponse<>(List.of(), 0, size, 0, 0);
        }
        String query = DomainSupport.trimToNull(q);
        var result = animals.search(
                SecurityUtils.organizationId(),
                status,
                sex,
                query,
                farmId,
                restrict && farmId == null,
                farmIds.isEmpty() ? List.of(DomainSupport.placeholderFarm()) : farmIds,
                Pages.of(page, size));
        Map<UUID, AnimalFarmHistory> current = currentFarms(result.getContent());
        Map<UUID, String> primary = primaryIdentifiers(result.getContent());
        return PageResponse.of(result.map(animal -> toResponse(
                animal,
                current.get(animal.getId()),
                primary.get(animal.getId()),
                false)));
    }

    public Animal readable(UUID id) {
        Animal animal = load(id);
        assertCanRead(animal);
        return animal;
    }

    public Animal requirePresent(UUID animalId, UUID farmId) {
        Animal animal = load(animalId);
        AnimalFarmHistory open = current(animalId);
        if (open == null || !open.getFarm().getId().equals(farmId)) {
            throw ApiException.unprocessable("Animal is not currently on this farm");
        }
        return animal;
    }

    public AnimalResponse get(UUID id) {
        Animal animal = load(id);
        assertCanRead(animal);
        return toResponse(animal, current(animal.getId()), primaryIdentifier(animal.getId()), true);
    }

    public AnimalResponse lookup(String code) {
        String value = code == null ? "" : code.trim();
        if (value.isEmpty()) {
            throw ApiException.badRequest("Enter an ear tag, RFID, or animal number");
        }
        UUID organizationId = SecurityUtils.organizationId();
        List<AnimalIdentifier> matches = identifiers.findActiveByValue(organizationId, value);
        if (!matches.isEmpty()) {
            AnimalIdentifier chosen = matches.stream()
                    .filter(AnimalIdentifier::isPrimary)
                    .findFirst()
                    .orElse(matches.get(0));
            return get(chosen.getAnimal().getId());
        }
        Animal byNumber = animals.findByOrganizationIdAndAnimalNumberIgnoreCase(organizationId, value)
                .orElseThrow(() -> ApiException.notFound("No cow matches this tag"));
        return get(byNumber.getId());
    }

    public List<HerdTagResponse> tagIndex(UUID farmId) {
        access.requireFarmRead(farmId);
        List<Animal> present = animals.findPresent(farmId);
        if (present.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<AnimalIdentifier>> tags = identifiers
                .findByAnimalIdInAndRetiredAtIsNull(present.stream().map(Animal::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(item -> item.getAnimal().getId()));
        return present.stream().map(animal -> new HerdTagResponse(
                animal.getId(),
                animal.getAnimalNumber(),
                animal.getName(),
                animal.getSex(),
                animal.getStatus(),
                farmId,
                tags.getOrDefault(animal.getId(), List.of()).stream()
                        .map(item -> new TagResponse(item.getIdentifierType().name(), item.getIdentifierValue()))
                        .toList()
        )).toList();
    }

    @Transactional
    public AnimalResponse create(CreateAnimalRequest request) {
        Farm farm = access.requireFarmWrite(request.farmId(), ANIMAL_WRITE);
        if (!farm.isActive()) {
            throw ApiException.unprocessable("This farm is not active");
        }
        Animal animal = new Animal();
        animal.setOrganization(farm.getOrganization());
        animal.setStatus(AnimalStatus.ACTIVE);
        animal.setCreatedBy(support.actor());
        apply(animal, request.speciesId(), request.breedId(), request.animalNumber(), request.name(), request.sex(),
                request.dateOfBirth(), request.birthDateEstimated(), request.colorMarkings(),
                request.acquisitionType(), request.acquiredOn(), request.damId(), request.sireId(),
                request.semenStrawId(), request.notes());
        animals.save(animal);

        LocalDate joinedOn = animal.getAcquiredOn() != null
                ? animal.getAcquiredOn()
                : animal.getDateOfBirth() != null ? animal.getDateOfBirth() : LocalDate.now();
        openHistory(animal, farm, joinedOn, reasonFor(request.acquisitionType()), request.notes());
        recordEvent(animal, farm, eventFor(request.acquisitionType()), joinedOn.atStartOfDay().toInstant(java.time.ZoneOffset.UTC), request.notes());
        if (request.identifier() != null) {
            addIdentifier(animal, request.identifier());
        }
        return toResponse(animal, current(animal.getId()), primaryIdentifier(animal.getId()), true);
    }

    @Transactional
    public AnimalResponse update(UUID id, UpdateAnimalRequest request) {
        Animal animal = load(id);
        assertCanWrite(animal);
        animal.setUpdatedBy(support.actor());
        apply(animal, request.speciesId(), request.breedId(), request.animalNumber(), request.name(), request.sex(),
                request.dateOfBirth(), request.birthDateEstimated(), request.colorMarkings(),
                request.acquisitionType(), request.acquiredOn(), request.damId(), request.sireId(),
                request.semenStrawId(), request.notes());
        return toResponse(animal, current(animal.getId()), primaryIdentifier(animal.getId()), true);
    }

    @Transactional
    public AnimalResponse changeStatus(UUID id, ChangeStatusRequest request) {
        Animal animal = load(id);
        assertCanWrite(animal);
        if (request.status() == AnimalStatus.ACTIVE && current(animal.getId()) == null) {
            throw ApiException.unprocessable("Transfer the animal onto a farm before marking it active");
        }
        AnimalFarmHistory open = current(animal.getId());
        if (leavesHerd(request.status())) {
            if (open != null) {
                if (request.statusChangedOn().isBefore(open.getJoinedOn())) {
                    throw ApiException.unprocessable("Status date cannot be before the animal joined the farm");
                }
                open.setLeftOn(request.statusChangedOn());
                support.flush();
            }
        }
        animal.setStatus(request.status());
        animal.setStatusChangedOn(request.statusChangedOn());
        animal.setUpdatedBy(support.actor());
        if (request.notes() != null && !request.notes().isBlank()) {
            animal.setNotes(request.notes().trim());
        }
        Farm farm = open == null ? null : open.getFarm();
        recordEvent(animal, farm, eventFor(request.status()),
                request.statusChangedOn().atStartOfDay().toInstant(java.time.ZoneOffset.UTC), request.notes());
        return toResponse(animal, current(animal.getId()), primaryIdentifier(animal.getId()), true);
    }

    @Transactional
    public AnimalResponse transfer(UUID id, TransferRequest request) {
        Animal animal = load(id);
        Farm destination = access.requireFarmWrite(request.toFarmId(), ANIMAL_WRITE);
        AnimalFarmHistory open = current(animal.getId());
        if (open != null) {
            access.requireFarmWrite(open.getFarm().getId(), ANIMAL_WRITE);
            if (open.getFarm().getId().equals(destination.getId())) {
                throw ApiException.unprocessable("The animal is already on this farm");
            }
            if (request.transferOn().isBefore(open.getJoinedOn())) {
                throw ApiException.unprocessable("Transfer date cannot be before the animal joined the current farm");
            }
            open.setLeftOn(request.transferOn());
            support.flush();
        }
        openHistory(animal, destination, request.transferOn(), TransferReason.TRANSFER_IN, request.notes());
        animal.setStatus(AnimalStatus.ACTIVE);
        animal.setStatusChangedOn(request.transferOn());
        animal.setUpdatedBy(support.actor());
        recordEvent(animal, destination, AnimalEventType.TRANSFER,
                request.transferOn().atStartOfDay().toInstant(java.time.ZoneOffset.UTC), request.notes());
        return toResponse(animal, current(animal.getId()), primaryIdentifier(animal.getId()), true);
    }

    public List<IdentifierResponse> identifiers(UUID animalId) {
        Animal animal = load(animalId);
        assertCanRead(animal);
        return identifiers.findByAnimalIdOrderByCreatedAtDesc(animalId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public IdentifierResponse addIdentifier(UUID animalId, IdentifierRequest request) {
        Animal animal = load(animalId);
        assertCanWrite(animal);
        return toResponse(addIdentifier(animal, request));
    }

    @Transactional
    public IdentifierResponse retireIdentifier(UUID id, RetireIdentifierRequest request) {
        AnimalIdentifier identifier = identifiers.findById(id)
                .orElseThrow(() -> ApiException.notFound("Identifier not found"));
        Animal animal = identifier.getAnimal();
        if (!animal.getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Identifier not found");
        }
        assertCanWrite(animal);
        if (identifier.getRetiredAt() != null) {
            throw ApiException.conflict("This identifier is already retired");
        }
        if (identifier.getIssuedAt() != null && request.retiredAt().isBefore(identifier.getIssuedAt())) {
            throw ApiException.unprocessable("Retired date cannot be before the issue date");
        }
        identifier.setRetiredAt(request.retiredAt());
        identifier.setPrimary(false);
        return toResponse(identifier);
    }

    public List<FarmHistoryResponse> farmHistory(UUID animalId) {
        Animal animal = load(animalId);
        assertCanRead(animal);
        return history.findByAnimalIdOrderByJoinedOnDesc(animalId).stream().map(this::toResponse).toList();
    }

    private AnimalIdentifier addIdentifier(Animal animal, IdentifierRequest request) {
        if (request.issuedAt() != null && request.primary()) {
            // primary flag is independent of issue date
        }
        if (request.primary()) {
            identifiers.clearPrimary(animal.getId());
        }
        AnimalIdentifier identifier = new AnimalIdentifier();
        identifier.setAnimal(animal);
        identifier.setIdentifierType(request.identifierType());
        identifier.setIdentifierValue(request.identifierValue().trim());
        identifier.setIssuingAuthority(DomainSupport.trimToNull(request.issuingAuthority()));
        identifier.setIssuedAt(request.issuedAt());
        identifier.setPrimary(request.primary());
        identifier.setNotes(DomainSupport.trimToNull(request.notes()));
        identifier.setCreatedBy(support.actor());
        return identifiers.save(identifier);
    }

    private void apply(
            Animal animal,
            Integer speciesId,
            UUID breedId,
            String animalNumber,
            String name,
            Sex sex,
            java.time.LocalDate dateOfBirth,
            boolean birthDateEstimated,
            String colorMarkings,
            AcquisitionType acquisitionType,
            java.time.LocalDate acquiredOn,
            UUID damId,
            UUID sireId,
            UUID semenStrawId,
            String notes) {
        animal.setSpecies(loadSpecies(speciesId));
        animal.setBreed(breedId == null ? null : loadBreed(breedId));
        animal.setAnimalNumber(animalNumber.trim());
        animal.setName(DomainSupport.trimToNull(name));
        animal.setSex(sex);
        animal.setDateOfBirth(dateOfBirth);
        animal.setBirthDateEstimated(birthDateEstimated);
        animal.setColorMarkings(DomainSupport.trimToNull(colorMarkings));
        animal.setAcquisitionType(acquisitionType);
        animal.setAcquiredOn(acquiredOn);
        Animal dam = damId == null ? null : loadParent(damId);
        Animal sire = sireId == null ? null : loadParent(sireId);
        SemenStraw straw = semenStrawId == null ? null : loadStraw(semenStrawId);
        if (straw != null && sire != null) {
            throw ApiException.unprocessable("Record either a bull on the farm or a straw as the father, not both");
        }
        if (straw != null && !straw.isActive()
                && (animal.getSemenStraw() == null || !animal.getSemenStraw().getId().equals(straw.getId()))) {
            throw ApiException.unprocessable("This straw is not active");
        }
        validateParents(animal, dam, sire);
        animal.setDam(dam);
        animal.setSire(straw == null ? sire : null);
        animal.setSemenStraw(straw);
        animal.setArtificialSireCode(straw == null ? null : straw.getCode());
        animal.setNotes(DomainSupport.trimToNull(notes));
        if (animal.getUpdatedBy() == null) {
            animal.setUpdatedBy(animal.getCreatedBy());
        }
    }

    private void validateParents(Animal animal, Animal dam, Animal sire) {
        UUID orgId = animal.getOrganization().getId();
        if (dam != null && sire != null && dam.getId().equals(sire.getId())) {
            throw ApiException.unprocessable("Dam and sire must be different animals");
        }
        checkParent(animal, dam, Sex.FEMALE, "Dam", orgId);
        checkParent(animal, sire, Sex.MALE, "Sire", orgId);
    }

    private void checkParent(Animal animal, Animal parent, Sex requiredSex, String label, UUID orgId) {
        if (parent == null) {
            return;
        }
        if (animal.getId() != null && animal.getId().equals(parent.getId())) {
            throw ApiException.unprocessable(label + " cannot be the animal itself");
        }
        if (parent.getSex() != requiredSex) {
            throw ApiException.unprocessable(label + " must be " + requiredSex.name().toLowerCase());
        }
        if (!parent.getOrganization().getId().equals(orgId)) {
            throw ApiException.unprocessable(label + " must belong to the same organization");
        }
        if (animal.getDateOfBirth() != null && parent.getDateOfBirth() != null
                && !parent.getDateOfBirth().isBefore(animal.getDateOfBirth())) {
            throw ApiException.unprocessable(label + " must be older than the offspring");
        }
    }

    private void openHistory(Animal animal, Farm farm, LocalDate joinedOn, TransferReason reason, String notes) {
        AnimalFarmHistory row = new AnimalFarmHistory();
        row.setAnimal(animal);
        row.setFarm(farm);
        row.setJoinedOn(joinedOn);
        row.setTransferReason(reason);
        row.setNotes(DomainSupport.trimToNull(notes));
        row.setTransferredBy(support.actor());
        history.save(row);
    }

    private void recordEvent(Animal animal, Farm farm, AnimalEventType type, Instant at, String details) {
        AnimalEvent event = new AnimalEvent();
        event.setAnimal(animal);
        event.setFarm(farm);
        event.setEventType(type);
        event.setEventAt(at);
        event.setDetails(DomainSupport.trimToNull(details));
        event.setRecordedBy(support.actor());
        events.save(event);
    }

    private Animal load(UUID id) {
        Animal animal = animals.findById(id).orElseThrow(() -> ApiException.notFound("Animal not found"));
        if (!animal.getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Animal not found");
        }
        return animal;
    }

    private void assertCanRead(Animal animal) {
        AnimalFarmHistory open = current(animal.getId());
        if (open != null) {
            access.requireFarmRead(open.getFarm().getId());
        } else {
            access.requireMember();
        }
    }

    private void assertCanWrite(Animal animal) {
        AnimalFarmHistory open = current(animal.getId());
        if (open != null) {
            access.requireFarmWrite(open.getFarm().getId(), ANIMAL_WRITE);
        } else {
            access.requireOrgAdmin();
        }
    }

    private AnimalFarmHistory current(UUID animalId) {
        return history.findByAnimalIdAndLeftOnIsNull(animalId).orElse(null);
    }

    private Map<UUID, AnimalFarmHistory> currentFarms(List<Animal> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<UUID, AnimalFarmHistory> mapped = new HashMap<>();
        for (AnimalFarmHistory row : history.findByAnimalIdInAndLeftOnIsNull(rows.stream().map(Animal::getId).toList())) {
            mapped.put(row.getAnimal().getId(), row);
        }
        return mapped;
    }

    private Map<UUID, String> primaryIdentifiers(List<Animal> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> mapped = new HashMap<>();
        for (AnimalIdentifier identifier : identifiers.findByAnimalIdInAndPrimaryTrueAndRetiredAtIsNull(
                rows.stream().map(Animal::getId).toList())) {
            mapped.put(identifier.getAnimal().getId(), identifier.getIdentifierValue());
        }
        return mapped;
    }

    private String primaryIdentifier(UUID animalId) {
        return identifiers.findByAnimalIdOrderByCreatedAtDesc(animalId).stream()
                .filter(identifier -> identifier.isPrimary() && identifier.getRetiredAt() == null)
                .map(AnimalIdentifier::getIdentifierValue)
                .findFirst()
                .orElse(null);
    }

    private AnimalSpecies loadSpecies(Integer speciesId) {
        if (speciesId < 1 || speciesId > Short.MAX_VALUE) {
            throw ApiException.badRequest("Species is not valid");
        }
        return speciesRepo.findById(speciesId.shortValue())
                .filter(AnimalSpecies::isActive)
                .orElseThrow(() -> ApiException.notFound("Species not found"));
    }

    private SemenStraw loadStraw(UUID id) {
        SemenStraw straw = straws.findById(id).orElseThrow(() -> ApiException.notFound("Straw not found"));
        if (!straw.getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Straw not found");
        }
        return straw;
    }

    private CattleBreed loadBreed(UUID id) {
        CattleBreed breed = breeds.findById(id).orElseThrow(() -> ApiException.notFound("Breed not found"));
        if (!breed.isActive()) {
            throw ApiException.unprocessable("This breed is not active");
        }
        return breed;
    }

    private Animal loadParent(UUID id) {
        return animals.findById(id).orElseThrow(() -> ApiException.notFound("Parent animal not found"));
    }

    private AnimalResponse toResponse(
            Animal animal, AnimalFarmHistory currentFarm, String primary, boolean includeParents) {
        return new AnimalResponse(
                animal.getId(),
                animal.getOrganization().getId(),
                animal.getSpecies().getId(),
                animal.getSpecies().getCode(),
                animal.getBreed() == null ? null : animal.getBreed().getId(),
                animal.getBreed() == null ? null : animal.getBreed().getName(),
                animal.getAnimalNumber(),
                animal.getName(),
                animal.getSex(),
                animal.getDateOfBirth(),
                animal.isBirthDateEstimated(),
                animal.getColorMarkings(),
                animal.getAcquisitionType(),
                animal.getAcquiredOn(),
                !includeParents || animal.getDam() == null ? null : animal.getDam().getId(),
                !includeParents || animal.getDam() == null ? null : animal.getDam().getAnimalNumber(),
                !includeParents || animal.getSire() == null ? null : animal.getSire().getId(),
                !includeParents || animal.getSire() == null ? null : animal.getSire().getAnimalNumber(),
                animal.getArtificialSireCode(),
                animal.getSemenStraw() == null ? null : animal.getSemenStraw().getId(),
                animal.getStatus(),
                animal.getStatusChangedOn(),
                animal.getNotes(),
                currentFarm == null ? null : currentFarm.getFarm().getId(),
                currentFarm == null ? null : currentFarm.getFarm().getName(),
                primary,
                animal.getCreatedAt(),
                animal.getUpdatedAt());
    }

    private IdentifierResponse toResponse(AnimalIdentifier identifier) {
        return new IdentifierResponse(
                identifier.getId(),
                identifier.getAnimal().getId(),
                identifier.getIdentifierType(),
                identifier.getIdentifierValue(),
                identifier.getIssuingAuthority(),
                identifier.getIssuedAt(),
                identifier.getRetiredAt(),
                identifier.isPrimary(),
                identifier.getNotes(),
                identifier.getCreatedAt());
    }

    private FarmHistoryResponse toResponse(AnimalFarmHistory row) {
        return new FarmHistoryResponse(
                row.getId(),
                row.getAnimal().getId(),
                row.getFarm().getId(),
                row.getFarm().getName(),
                row.getJoinedOn(),
                row.getLeftOn(),
                row.getTransferReason(),
                row.getNotes(),
                row.getRecordedAt());
    }

    private static boolean leavesHerd(AnimalStatus status) {
        return switch (status) {
            case SOLD, DECEASED, LOST, CULLED, TRANSFERRED_OUT -> true;
            case ACTIVE -> false;
        };
    }

    private static TransferReason reasonFor(AcquisitionType type) {
        return switch (type) {
            case BORN_ON_FARM -> TransferReason.BIRTH;
            case PURCHASED -> TransferReason.PURCHASE;
            case TRANSFERRED_IN -> TransferReason.TRANSFER_IN;
            case OTHER -> TransferReason.OTHER;
        };
    }

    private static AnimalEventType eventFor(AcquisitionType type) {
        return switch (type) {
            case BORN_ON_FARM -> AnimalEventType.BIRTH;
            case PURCHASED -> AnimalEventType.PURCHASE;
            case TRANSFERRED_IN -> AnimalEventType.TRANSFER;
            case OTHER -> AnimalEventType.OTHER;
        };
    }

    private static AnimalEventType eventFor(AnimalStatus status) {
        return switch (status) {
            case SOLD -> AnimalEventType.SALE;
            case DECEASED -> AnimalEventType.DEATH;
            case LOST -> AnimalEventType.LOSS;
            case TRANSFERRED_OUT -> AnimalEventType.TRANSFER;
            case CULLED, ACTIVE -> AnimalEventType.OTHER;
        };
    }
}
