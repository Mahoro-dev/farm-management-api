package rw.farm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.farm.breeding.Ancestry;
import rw.farm.common.ApiException;
import rw.farm.common.PageResponse;
import rw.farm.common.Pages;
import rw.farm.config.AppProperties;
import rw.farm.domain.Animal;
import rw.farm.domain.AnimalEvent;
import rw.farm.domain.BreedingRecord;
import rw.farm.domain.CalvingRecord;
import rw.farm.domain.CattleBreed;
import rw.farm.domain.Enums.AcquisitionType;
import rw.farm.domain.Enums.AnimalEventType;
import rw.farm.domain.Enums.AnimalStatus;
import rw.farm.domain.Enums.BreedingMethod;
import rw.farm.domain.Enums.BreedingOutcome;
import rw.farm.domain.Enums.CalvingOutcome;
import rw.farm.domain.Enums.PregnancyStatus;
import rw.farm.domain.Enums.RoleCode;
import rw.farm.domain.Enums.Sex;
import rw.farm.domain.Enums.TransferReason;
import rw.farm.domain.Farm;
import rw.farm.domain.PregnancyRecord;
import rw.farm.domain.SemenStraw;
import rw.farm.domain.User;
import rw.farm.dto.OperationDtos.BreedingResponse;
import rw.farm.dto.OperationDtos.CalvingResponse;
import rw.farm.dto.OperationDtos.CreateBreedingRequest;
import rw.farm.dto.OperationDtos.CreateCalvingRequest;
import rw.farm.dto.OperationDtos.CreateCalfRequest;
import rw.farm.dto.OperationDtos.PregnancyResponse;
import rw.farm.dto.OperationDtos.SavePregnancyRequest;
import rw.farm.dto.OperationDtos.UpdateBreedingRequest;
import rw.farm.repo.AnimalEventRepository;
import rw.farm.repo.AnimalFarmHistoryRepository;
import rw.farm.repo.AnimalRepository;
import rw.farm.repo.BreedingRecordRepository;
import rw.farm.repo.CalvingRecordRepository;
import rw.farm.repo.CattleBreedRepository;
import rw.farm.repo.PregnancyRecordRepository;
import rw.farm.repo.SemenStrawRepository;
import rw.farm.repo.UserRepository;
import rw.farm.security.SecurityUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static rw.farm.service.AccessService.BREEDING_WRITE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BreedingService {

    private static final int GESTATION_DAYS = 280;

    private final BreedingRecordRepository breedingRecords;
    private final PregnancyRecordRepository pregnancies;
    private final CalvingRecordRepository calvings;
    private final AnimalRepository animals;
    private final AnimalFarmHistoryRepository history;
    private final AnimalEventRepository events;
    private final CattleBreedRepository breeds;
    private final SemenStrawRepository straws;
    private final UserRepository users;
    private final AnimalService animalService;
    private final AccessService access;
    private final DomainSupport support;
    private final JdbcTemplate jdbc;
    private final AppProperties properties;

    public PageResponse<BreedingResponse> breeding(UUID farmId, int page, int size) {
        access.requireFarmRead(farmId);
        return PageResponse.of(breedingRecords.findByFarmIdOrderByBreedingAtDesc(farmId, Pages.of(page, size))
                .map(this::toResponse));
    }

    public PageResponse<BreedingResponse> breedingForCow(UUID cowId, int page, int size) {
        animalService.get(cowId);
        return PageResponse.of(breedingRecords.findByCowIdOrderByBreedingAtDesc(cowId, Pages.of(page, size))
                .map(this::toResponse));
    }

    @Transactional
    public BreedingResponse createBreeding(UUID farmId, CreateBreedingRequest request) {
        Farm farm = access.requireFarmWrite(farmId, BREEDING_WRITE);
        Animal cow = animalService.requirePresent(request.cowId(), farmId);
        BreedingRecord record = new BreedingRecord();
        record.setFarm(farm);
        record.setCow(cow);
        record.setRecordedBy(support.actor());
        record.setUpdatedBy(support.actor());
        record.setOutcome(request.outcome() == null ? BreedingOutcome.PENDING : request.outcome());
        applySireAndSchedule(record, request.sireId(), request.breedingAt(), request.breedingMethod(),
                request.technicianId(), request.semenStrawId(), request.ancestryOverrideReason(), request.notes());
        return toResponse(breedingRecords.save(record));
    }

    @Transactional
    public BreedingResponse updateBreeding(UUID id, UpdateBreedingRequest request) {
        BreedingRecord record = loadBreeding(id);
        access.requireFarmWrite(record.getFarm().getId(), BREEDING_WRITE);
        record.setUpdatedBy(support.actor());
        record.setOutcome(request.outcome());
        applySireAndSchedule(record, request.sireId(), request.breedingAt(), request.breedingMethod(),
                request.technicianId(), request.semenStrawId(), request.ancestryOverrideReason(), request.notes());
        return toResponse(record);
    }

    public PageResponse<PregnancyResponse> pregnancies(UUID farmId, int page, int size) {
        access.requireFarmRead(farmId);
        return PageResponse.of(pregnancies.findByFarmIdOrderByCreatedAtDesc(farmId, Pages.of(page, size)).map(this::toResponse));
    }

    public PageResponse<PregnancyResponse> pregnanciesForCow(UUID cowId, int page, int size) {
        animalService.get(cowId);
        return PageResponse.of(pregnancies.findByCowIdOrderByCreatedAtDesc(cowId, Pages.of(page, size)).map(this::toResponse));
    }

    @Transactional
    public PregnancyResponse createPregnancy(UUID farmId, SavePregnancyRequest request) {
        Farm farm = access.requireFarmWrite(farmId, BREEDING_WRITE);
        PregnancyRecord record = new PregnancyRecord();
        record.setFarm(farm);
        record.setRecordedBy(support.actor());
        record.setUpdatedBy(support.actor());
        apply(record, request);
        return toResponse(pregnancies.save(record));
    }

    @Transactional
    public PregnancyResponse updatePregnancy(UUID id, SavePregnancyRequest request) {
        PregnancyRecord record = loadPregnancy(id);
        access.requireFarmWrite(record.getFarm().getId(), BREEDING_WRITE);
        record.setUpdatedBy(support.actor());
        apply(record, request);
        return toResponse(record);
    }

    public PageResponse<CalvingResponse> calvings(UUID farmId, int page, int size) {
        access.requireFarmRead(farmId);
        return PageResponse.of(calvings.findByFarmIdOrderByCalvedAtDesc(farmId, Pages.of(page, size)).map(this::toResponse));
    }

    public PageResponse<CalvingResponse> calvingsForDam(UUID damId, int page, int size) {
        animalService.get(damId);
        return PageResponse.of(calvings.findByDamIdOrderByCalvedAtDesc(damId, Pages.of(page, size)).map(this::toResponse));
    }

    @Transactional
    public CalvingResponse createCalving(UUID farmId, CreateCalvingRequest request) {
        Farm farm = access.requireFarmWrite(farmId, BREEDING_WRITE);
        if (request.calfId() != null && request.createCalf() != null) {
            throw ApiException.badRequest("Provide an existing calf or details for a new calf, not both");
        }
        Animal dam = animalService.requirePresent(request.damId(), farmId);
        if (dam.getSex() != Sex.FEMALE) {
            throw ApiException.unprocessable("The dam must be female");
        }
        Animal sire = request.sireId() == null ? null : loadAnimal(request.sireId());
        SemenStraw straw = request.semenStrawId() == null ? null : loadStraw(request.semenStrawId());
        if (straw != null && sire != null) {
            throw ApiException.unprocessable("Record either a bull on the farm or a straw as the father, not both");
        }
        if (straw != null && !straw.isActive()) {
            throw ApiException.unprocessable("This straw is not active");
        }
        if (sire != null) {
            if (sire.getSex() != Sex.MALE) {
                throw ApiException.unprocessable("The sire must be male");
            }
            if (sire.getId().equals(dam.getId())) {
                throw ApiException.unprocessable("Dam and sire must be different animals");
            }
        }
        PregnancyRecord pregnancy = null;
        if (request.pregnancyRecordId() != null) {
            pregnancy = loadPregnancy(request.pregnancyRecordId());
            if (!pregnancy.getFarm().getId().equals(farmId) || !pregnancy.getCow().getId().equals(dam.getId())) {
                throw ApiException.unprocessable("Pregnancy record does not match this farm and dam");
            }
        }
        Animal calf = null;
        if (request.createCalf() != null) {
            calf = createCalf(farm, dam, straw == null ? sire : null, straw, request);
        } else if (request.calfId() != null) {
            calf = loadAnimal(request.calfId());
            linkExistingCalf(calf, dam, straw == null ? sire : null, straw);
        }
        CalvingRecord record = new CalvingRecord();
        record.setPregnancyRecord(pregnancy);
        record.setFarm(farm);
        record.setDam(dam);
        record.setSire(straw == null ? sire : null);
        record.setSemenStraw(straw);
        record.setArtificialSireCode(straw == null ? null : straw.getCode());
        record.setCalf(calf);
        record.setCalvedAt(request.calvedAt());
        record.setOutcome(request.outcome());
        record.setAssistanceRequired(request.assistanceRequired());
        record.setNumberOfCalves(request.numberOfCalves().shortValue());
        record.setComplications(DomainSupport.trimToNull(request.complications()));
        record.setRecordedBy(support.actor());
        calvings.save(record);
        if (pregnancy != null) {
            pregnancy.setStatus(request.outcome() == CalvingOutcome.ABORTION
                    ? PregnancyStatus.LOST
                    : PregnancyStatus.CALVED);
            pregnancy.setUpdatedBy(support.actor());
        }
        recordEvent(dam, farm, AnimalEventType.CALVING, request.calvedAt(), request.complications());
        return toResponse(record);
    }

    private void applySireAndSchedule(
            BreedingRecord record,
            UUID sireId,
            java.time.Instant breedingAt,
            rw.farm.domain.Enums.BreedingMethod method,
            UUID technicianId,
            UUID semenStrawId,
            String overrideReason,
            String notes) {
        Animal cow = record.getCow();
        if (cow.getSex() != Sex.FEMALE) {
            throw ApiException.unprocessable("Breeding requires a female cow");
        }
        boolean artificial = method == BreedingMethod.ARTIFICIAL_INSEMINATION || method == BreedingMethod.EMBRYO_TRANSFER;
        User technician = technicianId == null ? null : loadTechnician(technicianId, record);
        record.setTechnician(technician);
        record.setTechnicianName(technician == null ? null : technician.getFullName());
        if (artificial) {
            if (semenStrawId == null) {
                throw ApiException.unprocessable("Choose the straw. That straw is the father");
            }
            SemenStraw straw = loadStraw(semenStrawId);
            if (!straw.isActive() && (record.getSemenStraw() == null || !record.getSemenStraw().getId().equals(straw.getId()))) {
                throw ApiException.unprocessable("This straw is not active");
            }
            record.setSemenStraw(straw);
            record.setSemenBatchNumber(straw.getCode());
            record.setSire(null);
            record.setAncestryOverrideReason(null);
            record.setAncestryReviewedBy(null);
        } else {
            record.setSemenStraw(null);
            record.setSemenBatchNumber(null);
            if (sireId != null) {
                Animal sire = loadAnimal(sireId);
                reviewPair(cow, sire, overrideReason, record);
                record.setSire(sire);
            } else {
                record.setSire(null);
                record.setAncestryOverrideReason(null);
                record.setAncestryReviewedBy(null);
            }
        }
        record.setBreedingAt(breedingAt);
        record.setBreedingMethod(method);
        record.setNotes(DomainSupport.trimToNull(notes));
    }

    private void reviewPair(Animal cow, Animal sire, String overrideReason, BreedingRecord record) {
        if (sire.getSex() != Sex.MALE) {
            throw ApiException.unprocessable("The sire must be male");
        }
        if (sire.getId().equals(cow.getId())) {
            throw ApiException.unprocessable("Cow and sire must be different animals");
        }
        if (!sire.getOrganization().getId().equals(cow.getOrganization().getId())) {
            throw ApiException.unprocessable("Cow and sire must belong to the same organization");
        }
        String reason = DomainSupport.trimToNull(overrideReason);
        int generations = generations();
        if (reason != null) {
            if (!access.isOrgAdmin() && !access.hasAnyRole(RoleCode.VETERINARIAN)) {
                throw ApiException.forbidden("Only an organization administrator or veterinarian can override ancestry");
            }
            record.setAncestryOverrideReason(reason);
            record.setAncestryReviewedBy(support.actor());
            return;
        }
        var links = Ancestry.linksFrom(animals.parentLinks(cow.getOrganization().getId()));
        if (Ancestry.sharesAncestor(cow.getId(), sire.getId(), links, generations)) {
            throw ApiException.unprocessable(
                    "Breeding rejected: shared ancestor found within " + generations + " generations");
        }
        try {
            jdbc.update("CALL check_breeding_compatibility(?, ?, ?)",
                    cow.getId().toString(), sire.getId().toString(), generations);
        } catch (DataAccessException ex) {
            throw ApiException.unprocessable(signalMessage(ex));
        }
        record.setAncestryOverrideReason(null);
        record.setAncestryReviewedBy(null);
    }

    private void apply(PregnancyRecord record, SavePregnancyRequest request) {
        Animal cow = animalService.requirePresent(request.cowId(), record.getFarm().getId());
        if (cow.getSex() != Sex.FEMALE) {
            throw ApiException.unprocessable("Pregnancy requires a female animal");
        }
        BreedingRecord breeding = null;
        if (request.breedingRecordId() != null) {
            breeding = loadBreeding(request.breedingRecordId());
            if (!breeding.getFarm().getId().equals(record.getFarm().getId())
                    || !breeding.getCow().getId().equals(cow.getId())) {
                throw ApiException.unprocessable("Breeding record does not match this farm and cow");
            }
        }
        record.setCow(cow);
        record.setBreedingRecord(breeding);
        record.setConfirmedAt(request.confirmedAt());
        record.setConfirmationMethod(DomainSupport.trimToNull(request.confirmationMethod()));
        LocalDate expected = request.expectedCalvingOn();
        if (expected == null && breeding != null) {
            expected = breeding.getBreedingAt().atZone(ZoneOffset.UTC).toLocalDate().plusDays(GESTATION_DAYS);
        }
        if (expected != null && request.confirmedAt() != null
                && expected.isBefore(request.confirmedAt().atZone(ZoneOffset.UTC).toLocalDate())) {
            throw ApiException.unprocessable("Expected calving date cannot be before confirmation");
        }
        record.setExpectedCalvingOn(expected);
        PregnancyStatus status = request.status() == null ? PregnancyStatus.SUSPECTED : request.status();
        record.setStatus(status);
        record.setNotes(DomainSupport.trimToNull(request.notes()));
        if (breeding != null && status == PregnancyStatus.CONFIRMED) {
            breeding.setOutcome(BreedingOutcome.PREGNANT);
            breeding.setUpdatedBy(support.actor());
        } else if (breeding != null && status == PregnancyStatus.FALSE_PREGNANCY) {
            breeding.setOutcome(BreedingOutcome.NOT_PREGNANT);
            breeding.setUpdatedBy(support.actor());
        }
    }

    private Animal createCalf(Farm farm, Animal dam, Animal sire, SemenStraw straw, CreateCalvingRequest request) {
        CreateCalfRequest calfRequest = request.createCalf();
        Animal calf = new Animal();
        calf.setOrganization(dam.getOrganization());
        calf.setSpecies(dam.getSpecies());
        if (calfRequest.breedId() != null) {
            CattleBreed breed = breeds.findById(calfRequest.breedId())
                    .orElseThrow(() -> ApiException.notFound("Breed not found"));
            calf.setBreed(breed);
        } else {
            calf.setBreed(dam.getBreed());
        }
        calf.setAnimalNumber(calfRequest.animalNumber().trim());
        calf.setName(DomainSupport.trimToNull(calfRequest.name()));
        calf.setSex(calfRequest.sex());
        calf.setDateOfBirth(request.calvedAt().atZone(ZoneOffset.UTC).toLocalDate());
        calf.setBirthDateEstimated(false);
        calf.setColorMarkings(DomainSupport.trimToNull(calfRequest.colorMarkings()));
        calf.setAcquisitionType(AcquisitionType.BORN_ON_FARM);
        calf.setAcquiredOn(calf.getDateOfBirth());
        calf.setDam(dam);
        calf.setSire(sire);
        calf.setSemenStraw(straw);
        calf.setArtificialSireCode(straw == null ? null : straw.getCode());
        calf.setStatus(AnimalStatus.ACTIVE);
        calf.setCreatedBy(support.actor());
        calf.setUpdatedBy(support.actor());
        animals.saveAndFlush(calf);

        rw.farm.domain.AnimalFarmHistory row = new rw.farm.domain.AnimalFarmHistory();
        row.setAnimal(calf);
        row.setFarm(farm);
        row.setJoinedOn(calf.getDateOfBirth());
        row.setTransferReason(TransferReason.BIRTH);
        row.setTransferredBy(support.actor());
        history.save(row);
        recordEvent(calf, farm, AnimalEventType.BIRTH, request.calvedAt(), "Born on farm");
        return calf;
    }

    private void linkExistingCalf(Animal calf, Animal dam, Animal sire, SemenStraw straw) {
        if (calf.getId().equals(dam.getId()) || (sire != null && calf.getId().equals(sire.getId()))) {
            throw ApiException.unprocessable("Calf cannot be the dam or the sire");
        }
        if (calf.getDam() != null && !calf.getDam().getId().equals(dam.getId())) {
            throw ApiException.unprocessable("Calf is already linked to a different dam");
        }
        if (straw != null) {
            if (calf.getSire() != null) {
                throw ApiException.unprocessable("Calf is already linked to a bull on the farm");
            }
            if (calf.getSemenStraw() != null && !calf.getSemenStraw().getId().equals(straw.getId())) {
                throw ApiException.unprocessable("Calf is already linked to a different straw");
            }
            calf.setSemenStraw(straw);
            calf.setArtificialSireCode(straw.getCode());
            calf.setSire(null);
        } else if (sire != null) {
            if (calf.getSemenStraw() != null) {
                throw ApiException.unprocessable("Calf is already linked to an artificial father");
            }
            if (calf.getSire() != null && !calf.getSire().getId().equals(sire.getId())) {
                throw ApiException.unprocessable("Calf is already linked to a different sire");
            }
            calf.setSire(sire);
        }
        calf.setDam(dam);
        calf.setUpdatedBy(support.actor());
    }

    private void recordEvent(Animal animal, Farm farm, AnimalEventType type, java.time.Instant at, String details) {
        AnimalEvent event = new AnimalEvent();
        event.setAnimal(animal);
        event.setFarm(farm);
        event.setEventType(type);
        event.setEventAt(at);
        event.setDetails(DomainSupport.trimToNull(details));
        event.setRecordedBy(support.actor());
        events.save(event);
    }

    private int generations() {
        int generations = properties.breeding().maxGenerations();
        if (generations < 1 || generations > 20) {
            throw ApiException.unprocessable("Breeding generation limit must be between 1 and 20");
        }
        return generations;
    }

    private String signalMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sql && sql.getMessage() != null) {
                return sql.getMessage();
            }
            current = current.getCause();
        }
        return "Breeding rejected";
    }

    private User loadTechnician(UUID id, BreedingRecord record) {
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("Person not found"));
        boolean alreadyChosen = record.getTechnician() != null && record.getTechnician().getId().equals(id);
        if (!user.getOrganization().getId().equals(SecurityUtils.organizationId()) || (!user.isActive() && !alreadyChosen)) {
            throw ApiException.unprocessable("Choose an active person from this organization");
        }
        return user;
    }

    private SemenStraw loadStraw(UUID id) {
        SemenStraw straw = straws.findById(id).orElseThrow(() -> ApiException.notFound("Straw not found"));
        if (!straw.getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Straw not found");
        }
        return straw;
    }

    private Animal loadAnimal(UUID id) {
        Animal animal = animals.findById(id).orElseThrow(() -> ApiException.notFound("Animal not found"));
        if (!animal.getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Animal not found");
        }
        return animal;
    }

    private BreedingRecord loadBreeding(UUID id) {
        BreedingRecord record = breedingRecords.findById(id)
                .orElseThrow(() -> ApiException.notFound("Breeding record not found"));
        if (!record.getFarm().getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Breeding record not found");
        }
        return record;
    }

    private PregnancyRecord loadPregnancy(UUID id) {
        PregnancyRecord record = pregnancies.findById(id)
                .orElseThrow(() -> ApiException.notFound("Pregnancy record not found"));
        if (!record.getFarm().getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Pregnancy record not found");
        }
        return record;
    }

    private BreedingResponse toResponse(BreedingRecord record) {
        return new BreedingResponse(
                record.getId(),
                record.getFarm().getId(),
                record.getCow().getId(),
                record.getCow().getAnimalNumber(),
                record.getSire() == null ? null : record.getSire().getId(),
                record.getSire() == null ? null : record.getSire().getAnimalNumber(),
                record.getBreedingAt(),
                record.getBreedingMethod(),
                record.getOutcome(),
                record.getTechnicianName(),
                record.getTechnician() == null ? null : record.getTechnician().getId(),
                record.getSemenBatchNumber(),
                record.getSemenStraw() == null ? null : record.getSemenStraw().getId(),
                record.getAncestryReviewedBy() == null ? null : record.getAncestryReviewedBy().getId(),
                record.getAncestryOverrideReason(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private PregnancyResponse toResponse(PregnancyRecord record) {
        return new PregnancyResponse(
                record.getId(),
                record.getBreedingRecord() == null ? null : record.getBreedingRecord().getId(),
                record.getFarm().getId(),
                record.getCow().getId(),
                record.getCow().getAnimalNumber(),
                record.getConfirmedAt(),
                record.getConfirmationMethod(),
                record.getExpectedCalvingOn(),
                record.getStatus(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private CalvingResponse toResponse(CalvingRecord record) {
        return new CalvingResponse(
                record.getId(),
                record.getPregnancyRecord() == null ? null : record.getPregnancyRecord().getId(),
                record.getFarm().getId(),
                record.getDam().getId(),
                record.getDam().getAnimalNumber(),
                record.getSire() == null ? null : record.getSire().getId(),
                record.getSire() == null ? null : record.getSire().getAnimalNumber(),
                record.getArtificialSireCode(),
                record.getSemenStraw() == null ? null : record.getSemenStraw().getId(),
                record.getCalf() == null ? null : record.getCalf().getId(),
                record.getCalf() == null ? null : record.getCalf().getAnimalNumber(),
                record.getCalvedAt(),
                record.getOutcome(),
                record.isAssistanceRequired(),
                record.getNumberOfCalves(),
                record.getComplications(),
                record.getCreatedAt());
    }
}
