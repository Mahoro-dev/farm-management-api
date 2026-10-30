package rw.farm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.farm.common.ApiException;
import rw.farm.common.PageResponse;
import rw.farm.common.Pages;
import rw.farm.domain.Animal;
import rw.farm.domain.Enums.AnimalStatus;
import rw.farm.domain.Enums.Sex;
import rw.farm.domain.Farm;
import rw.farm.domain.LactationStage;
import rw.farm.domain.MilkRecord;
import rw.farm.domain.MilkingShift;
import rw.farm.dto.OperationDtos.LactationStageResponse;
import rw.farm.dto.OperationDtos.MilkResponse;
import rw.farm.dto.OperationDtos.SaveMilkRequest;
import rw.farm.dto.OperationDtos.SaveShiftRequest;
import rw.farm.dto.OperationDtos.ShiftResponse;
import rw.farm.repo.LactationStageRepository;
import rw.farm.repo.MilkRecordRepository;
import rw.farm.repo.MilkingShiftRepository;
import rw.farm.security.SecurityUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static rw.farm.service.AccessService.MILK_WRITE;
import static rw.farm.service.AccessService.SHIFT_WRITE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MilkService {

    private final MilkingShiftRepository shifts;
    private final MilkRecordRepository records;
    private final LactationStageRepository stages;
    private final AnimalService animals;
    private final AccessService access;
    private final DomainSupport support;

    public List<LactationStageResponse> stages() {
        return stages.findAllByOrderByDisplayOrderAsc().stream()
                .map(stage -> new LactationStageResponse(stage.getCode(), stage.getName(), stage.getMeaning(), stage.getDisplayOrder()))
                .toList();
    }

    public List<ShiftResponse> shifts(UUID farmId) {
        access.requireFarmRead(farmId);
        return shifts.findByFarmIdOrderByDisplayOrderAscNameAsc(farmId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ShiftResponse createShift(UUID farmId, SaveShiftRequest request) {
        Farm farm = access.requireFarmWrite(farmId, SHIFT_WRITE);
        validateTimes(request.startTime(), request.endTime());
        MilkingShift shift = new MilkingShift();
        shift.setFarm(farm);
        shift.setCreatedBy(support.actor());
        apply(shift, request, true);
        return toResponse(shifts.save(shift));
    }

    @Transactional
    public ShiftResponse updateShift(UUID id, SaveShiftRequest request) {
        MilkingShift shift = loadShift(id);
        access.requireFarmWrite(shift.getFarm().getId(), SHIFT_WRITE);
        validateTimes(request.startTime(), request.endTime());
        apply(shift, request, false);
        return toResponse(shift);
    }

    public PageResponse<MilkResponse> records(UUID farmId, LocalDate from, LocalDate to, int page, int size) {
        access.requireFarmRead(farmId);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        if (start.isAfter(end)) {
            throw ApiException.badRequest("from cannot be after to");
        }
        return PageResponse.of(records
                .findByFarmIdAndMilkingDateBetweenOrderByMilkingDateDescCreatedAtDesc(farmId, start, end, Pages.of(page, size))
                .map(this::toResponse));
    }

    public PageResponse<MilkResponse> recordsForAnimal(UUID animalId, int page, int size) {
        animals.get(animalId);
        return PageResponse.of(records.findByAnimalIdOrderByMilkingDateDesc(animalId, Pages.of(page, size))
                .map(this::toResponse));
    }

    @Transactional
    public MilkResponse createRecord(UUID farmId, SaveMilkRequest request) {
        Farm farm = access.requireFarmWrite(farmId, MILK_WRITE);
        return toResponse(saveNew(farm, request));
    }

    @Transactional
    public MilkResponse updateRecord(UUID id, SaveMilkRequest request) {
        MilkRecord record = records.findById(id).orElseThrow(() -> ApiException.notFound("Milk record not found"));
        Farm farm = access.requireFarmWrite(record.getFarm().getId(), MILK_WRITE);
        if (!record.getFarm().getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Milk record not found");
        }
        Animal animal = animals.requirePresent(request.animalId(), farm.getId());
        assertMilkable(animal);
        MilkingShift shift = loadShift(request.shiftId());
        if (!shift.getFarm().getId().equals(farm.getId())) {
            throw ApiException.unprocessable("Milking shift does not belong to this farm");
        }
        record.setAnimal(animal);
        record.setShift(shift);
        record.setMilkingDate(request.milkingDate() == null ? LocalDate.now() : request.milkingDate());
        record.setQuantityLiters(request.quantityLiters());
        record.setLactationStage(loadStage(request.lactationStage()));
        record.setFatPercent(request.fatPercent());
        record.setProteinPercent(request.proteinPercent());
        record.setSomaticCellCount(request.somaticCellCount());
        record.setNotes(DomainSupport.trimToNull(request.notes()));
        record.setUpdatedBy(support.actor());
        return toResponse(record);
    }

    private MilkRecord saveNew(Farm farm, SaveMilkRequest request) {
        Animal animal = animals.requirePresent(request.animalId(), farm.getId());
        assertMilkable(animal);
        MilkingShift shift = loadShift(request.shiftId());
        if (!shift.getFarm().getId().equals(farm.getId())) {
            throw ApiException.unprocessable("Milking shift does not belong to this farm");
        }
        if (!shift.isActive()) {
            throw ApiException.unprocessable("Milking shift is not active");
        }
        MilkRecord record = new MilkRecord();
        record.setFarm(farm);
        record.setAnimal(animal);
        record.setShift(shift);
        record.setMilkingDate(request.milkingDate() == null ? LocalDate.now() : request.milkingDate());
        record.setQuantityLiters(request.quantityLiters());
        record.setLactationStage(loadStage(request.lactationStage()));
        record.setFatPercent(request.fatPercent());
        record.setProteinPercent(request.proteinPercent());
        record.setSomaticCellCount(request.somaticCellCount());
        record.setNotes(DomainSupport.trimToNull(request.notes()));
        record.setCreatedBy(support.actor());
        record.setUpdatedBy(support.actor());
        return records.save(record);
    }

    private void assertMilkable(Animal animal) {
        if (animal.getSex() != Sex.FEMALE) {
            throw ApiException.unprocessable("Milk can only be recorded for a female animal");
        }
        if (animal.getStatus() != AnimalStatus.ACTIVE) {
            throw ApiException.unprocessable("Milk can only be recorded for an active animal");
        }
    }

    private void apply(MilkingShift shift, SaveShiftRequest request, boolean creating) {
        shift.setName(request.name().trim());
        shift.setStartTime(request.startTime());
        shift.setEndTime(request.endTime());
        shift.setDisplayOrder(request.displayOrder().shortValue());
        if (request.active() != null) {
            shift.setActive(request.active());
        } else if (creating) {
            shift.setActive(true);
        }
    }

    private void validateTimes(LocalTime start, LocalTime end) {
        if (start != null && end != null && start.equals(end)) {
            throw ApiException.unprocessable("Shift start and end must be different");
        }
    }

    private LactationStage loadStage(String code) {
        String key = code == null ? "" : code.trim();
        return stages.findById(key).orElseThrow(() -> ApiException.unprocessable("Choose a lactation stage"));
    }

    private MilkingShift loadShift(UUID id) {
        MilkingShift shift = shifts.findById(id).orElseThrow(() -> ApiException.notFound("Milking shift not found"));
        if (!shift.getFarm().getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Milking shift not found");
        }
        return shift;
    }

    private ShiftResponse toResponse(MilkingShift shift) {
        return new ShiftResponse(
                shift.getId(), shift.getFarm().getId(), shift.getName(), shift.getStartTime(),
                shift.getEndTime(), shift.getDisplayOrder(), shift.isActive());
    }

    private MilkResponse toResponse(MilkRecord record) {
        return new MilkResponse(
                record.getId(),
                record.getFarm().getId(),
                record.getAnimal().getId(),
                record.getAnimal().getAnimalNumber(),
                record.getShift().getId(),
                record.getShift().getName(),
                record.getMilkingDate(),
                record.getQuantityLiters(),
                record.getLactationStage() == null ? null : record.getLactationStage().getCode(),
                record.getLactationStage() == null ? null : record.getLactationStage().getName(),
                record.getFatPercent(),
                record.getProteinPercent(),
                record.getSomaticCellCount(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
