package rw.farm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.farm.common.ApiException;
import rw.farm.common.PageResponse;
import rw.farm.common.Pages;
import rw.farm.domain.Animal;
import rw.farm.domain.AnimalWeightRecord;
import rw.farm.domain.Enums.StockMovementType;
import rw.farm.domain.Enums.VisitType;
import rw.farm.domain.Farm;
import rw.farm.domain.MedicineBatch;
import rw.farm.domain.MedicineProduct;
import rw.farm.domain.MedicineStockMovement;
import rw.farm.domain.Treatment;
import rw.farm.domain.TreatmentMedication;
import rw.farm.domain.Vaccination;
import rw.farm.domain.VeterinaryVisit;
import rw.farm.dto.OperationDtos.AddMedicationRequest;
import rw.farm.dto.OperationDtos.CreateVaccinationRequest;
import rw.farm.dto.OperationDtos.MedicationResponse;
import rw.farm.dto.OperationDtos.SaveTreatmentRequest;
import rw.farm.dto.OperationDtos.SaveVisitRequest;
import rw.farm.dto.OperationDtos.TreatmentResponse;
import rw.farm.dto.OperationDtos.VaccinationResponse;
import rw.farm.dto.OperationDtos.VisitResponse;
import rw.farm.repo.AnimalWeightRecordRepository;
import rw.farm.repo.MedicineBatchRepository;
import rw.farm.repo.MedicineProductRepository;
import rw.farm.repo.MedicineStockMovementRepository;
import rw.farm.repo.TreatmentMedicationRepository;
import rw.farm.repo.TreatmentRepository;
import rw.farm.repo.VaccinationRepository;
import rw.farm.repo.VeterinaryVisitRepository;
import rw.farm.security.SecurityUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static rw.farm.service.AccessService.CLINICAL_WRITE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicalService {

    private final VeterinaryVisitRepository visits;
    private final VaccinationRepository vaccinations;
    private final TreatmentRepository treatments;
    private final TreatmentMedicationRepository medications;
    private final MedicineProductRepository products;
    private final MedicineBatchRepository batches;
    private final MedicineStockMovementRepository movements;
    private final AnimalWeightRecordRepository weights;
    private final AnimalService animals;
    private final AccessService access;
    private final DomainSupport support;

    public PageResponse<VisitResponse> visits(UUID farmId, int page, int size) {
        access.requireFarmRead(farmId);
        return PageResponse.of(visits.findByFarmIdOrderByVisitAtDesc(farmId, Pages.of(page, size)).map(this::toResponse));
    }

    public PageResponse<VisitResponse> visitsForAnimal(UUID animalId, int page, int size) {
        animals.get(animalId);
        return PageResponse.of(visits.findByAnimalIdOrderByVisitAtDesc(animalId, Pages.of(page, size)).map(this::toResponse));
    }

    public VisitResponse visit(UUID id) {
        VeterinaryVisit visit = loadVisit(id);
        access.requireFarmRead(visit.getFarm().getId());
        return toResponse(visit);
    }

    @Transactional
    public VisitResponse createVisit(UUID farmId, SaveVisitRequest request) {
        Farm farm = access.requireFarmWrite(farmId, CLINICAL_WRITE);
        if (request.visitAt() != null) {
            VisitType visitType = request.visitType() == null ? VisitType.EXAMINATION : request.visitType();
            var existing = visits.findFirstByFarmIdAndAnimalIdAndVisitAtAndVisitType(
                    farmId, request.animalId(), request.visitAt(), visitType);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }
        VeterinaryVisit visit = new VeterinaryVisit();
        visit.setFarm(farm);
        visit.setRecordedBy(support.actor());
        visit.setUpdatedBy(support.actor());
        apply(visit, request);
        visits.save(visit);
        if (request.weightKg() != null) {
            recordWeight(visit);
        }
        return toResponse(visit);
    }

    @Transactional
    public VisitResponse updateVisit(UUID id, SaveVisitRequest request) {
        VeterinaryVisit visit = loadVisit(id);
        access.requireFarmWrite(visit.getFarm().getId(), CLINICAL_WRITE);
        visit.setUpdatedBy(support.actor());
        apply(visit, request);
        return toResponse(visit);
    }

    public PageResponse<VaccinationResponse> vaccinations(UUID farmId, int page, int size) {
        access.requireFarmRead(farmId);
        return PageResponse.of(vaccinations.findByFarmIdOrderByAdministeredAtDesc(farmId, Pages.of(page, size))
                .map(this::toResponse));
    }

    public PageResponse<VaccinationResponse> vaccinationsForAnimal(UUID animalId, int page, int size) {
        animals.get(animalId);
        return PageResponse.of(vaccinations.findByAnimalIdOrderByAdministeredAtDesc(animalId, Pages.of(page, size))
                .map(this::toResponse));
    }

    public List<VaccinationResponse> due(UUID farmId, LocalDate until) {
        access.requireFarmRead(farmId);
        LocalDate horizon = until == null ? LocalDate.now().plusDays(30) : until;
        return vaccinations.dueBy(farmId, horizon).stream().map(this::toResponse).toList();
    }

    @Transactional
    public VaccinationResponse createVaccination(UUID farmId, CreateVaccinationRequest request) {
        Farm farm = access.requireFarmWrite(farmId, CLINICAL_WRITE);
        Animal animal = animals.requirePresent(request.animalId(), farmId);
        Vaccination vaccination = new Vaccination();
        vaccination.setFarm(farm);
        vaccination.setAnimal(animal);
        if (request.veterinaryVisitId() != null) {
            VeterinaryVisit visit = loadVisit(request.veterinaryVisitId());
            if (!visit.getFarm().getId().equals(farmId) || !visit.getAnimal().getId().equals(animal.getId())) {
                throw ApiException.unprocessable("Veterinary visit does not match this farm and animal");
            }
            vaccination.setVeterinaryVisit(visit);
        }
        MedicineProduct product = loadProduct(request.medicineProductId());
        LocalDate givenOn = (request.administeredAt() == null ? Instant.now() : request.administeredAt())
                .atZone(ZoneOffset.UTC).toLocalDate();
        MedicineBatch batch = requireStock(farmId, product, request.medicineBatchId(), request.quantityUsed(), givenOn);
        vaccination.setMedicineProduct(product);
        vaccination.setMedicineBatch(batch);
        vaccination.setQuantityUsed(request.quantityUsed());
        vaccination.setVaccineName(product.getName());
        vaccination.setBatchNumber(batch.getBatchNumber());
        vaccination.setDose(request.quantityUsed().stripTrailingZeros().toPlainString() + " " + product.getBaseUnit().name());
        vaccination.setAdministeredAt(request.administeredAt() == null ? Instant.now() : request.administeredAt());
        vaccination.setNextDueOn(request.nextDueOn());
        vaccination.setNotes(DomainSupport.trimToNull(request.notes()));
        vaccination.setAdministeredBy(support.actor());
        if (vaccination.getNextDueOn() != null
                && vaccination.getNextDueOn().isBefore(vaccination.getAdministeredAt().atZone(ZoneOffset.UTC).toLocalDate())) {
            throw ApiException.unprocessable("Next due date cannot be before the vaccination date");
        }
        vaccinations.saveAndFlush(vaccination);
        MedicineStockMovement movement = new MedicineStockMovement();
        movement.setBatch(batch);
        movement.setMovementType(StockMovementType.VACCINATION_USE);
        movement.setQuantityChange(request.quantityUsed().negate());
        movement.setMovementAt(vaccination.getAdministeredAt());
        movement.setVaccination(vaccination);
        movement.setReferenceText(animal.getAnimalNumber());
        movement.setRecordedBy(support.actor());
        movements.saveAndFlush(movement);
        support.refresh(batch);
        return toResponse(vaccination);
    }

    public PageResponse<TreatmentResponse> treatments(UUID farmId, int page, int size) {
        access.requireFarmRead(farmId);
        return PageResponse.of(treatments.findByFarmIdOrderByTreatmentDateDesc(farmId, Pages.of(page, size))
                .map(treatment -> toResponse(treatment, false)));
    }

    public PageResponse<TreatmentResponse> treatmentsForAnimal(UUID animalId, int page, int size) {
        animals.get(animalId);
        return PageResponse.of(treatments.findByAnimalIdOrderByTreatmentDateDesc(animalId, Pages.of(page, size))
                .map(treatment -> toResponse(treatment, false)));
    }

    public TreatmentResponse treatment(UUID id) {
        Treatment treatment = loadTreatment(id);
        access.requireFarmRead(treatment.getFarm().getId());
        return toResponse(treatment, true);
    }

    @Transactional
    public TreatmentResponse createTreatment(UUID farmId, SaveTreatmentRequest request) {
        Farm farm = access.requireFarmWrite(farmId, CLINICAL_WRITE);
        Treatment treatment = new Treatment();
        treatment.setFarm(farm);
        treatment.setRecordedBy(support.actor());
        treatment.setUpdatedBy(support.actor());
        apply(treatment, request);
        return toResponse(treatments.save(treatment), true);
    }

    @Transactional
    public TreatmentResponse updateTreatment(UUID id, SaveTreatmentRequest request) {
        Treatment treatment = loadTreatment(id);
        access.requireFarmWrite(treatment.getFarm().getId(), CLINICAL_WRITE);
        treatment.setUpdatedBy(support.actor());
        apply(treatment, request);
        return toResponse(treatment, true);
    }

    @Transactional
    public MedicationResponse addMedication(UUID treatmentId, AddMedicationRequest request) {
        Treatment treatment = loadTreatment(treatmentId);
        access.requireFarmWrite(treatment.getFarm().getId(), CLINICAL_WRITE);
        MedicineProduct product = loadProduct(request.medicineProductId());
        LocalDate treatmentDay = treatment.getTreatmentDate().atZone(ZoneOffset.UTC).toLocalDate();
        MedicineBatch batch = requireStock(
                treatment.getFarm().getId(), product, request.batchId(), request.quantityUsed(), treatmentDay);
        TreatmentMedication medication = new TreatmentMedication();
        medication.setTreatment(treatment);
        medication.setMedicineProduct(product);
        medication.setBatch(batch);
        medication.setDoseAmount(request.doseAmount());
        medication.setDoseUnit(request.doseUnit().trim());
        medication.setRoute(request.route());
        medication.setAdministrationsCount(request.administrationsCount().shortValue());
        medication.setQuantityUsed(request.quantityUsed());
        medication.setFrequency(DomainSupport.trimToNull(request.frequency()));
        medication.setDurationText(DomainSupport.trimToNull(request.durationText()));
        medication.setMilkWithdrawalHours(request.milkWithdrawalHours());
        medication.setMeatWithdrawalHours(request.meatWithdrawalHours());
        medication.setNotes(DomainSupport.trimToNull(request.notes()));
        medication.setCreatedBy(support.actor());
        medications.saveAndFlush(medication);
        extendWithdrawal(treatment, request.milkWithdrawalHours(), request.meatWithdrawalHours());
        MedicineStockMovement movement = new MedicineStockMovement();
        movement.setBatch(batch);
        movement.setMovementType(StockMovementType.TREATMENT_USE);
        movement.setQuantityChange(request.quantityUsed().negate());
        movement.setMovementAt(treatment.getTreatmentDate());
        movement.setTreatmentMedication(medication);
        movement.setNotes(DomainSupport.trimToNull(request.notes()));
        movement.setRecordedBy(support.actor());
        movements.saveAndFlush(movement);
        support.refresh(batch);
        return toResponse(medication);
    }

    private MedicineProduct loadProduct(UUID id) {
        MedicineProduct product = products.findById(id)
                .orElseThrow(() -> ApiException.notFound("Medicine product not found"));
        if (!product.getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Medicine product not found");
        }
        if (!product.isActive()) {
            throw ApiException.unprocessable("Medicine product is not active");
        }
        return product;
    }

    private MedicineBatch requireStock(UUID farmId, MedicineProduct product, UUID batchId, java.math.BigDecimal quantity, LocalDate onDay) {
        MedicineBatch batch = batches.findById(batchId)
                .orElseThrow(() -> ApiException.notFound("Medicine batch not found"));
        if (!batch.getFarm().getId().equals(farmId)) {
            throw ApiException.unprocessable("Medicine batch is not stocked on this farm");
        }
        if (!batch.getMedicineProduct().getId().equals(product.getId())) {
            throw ApiException.unprocessable("Medicine batch does not match the selected product");
        }
        if (batch.getExpiryDate().isBefore(onDay)) {
            throw ApiException.unprocessable("Medicine batch is expired");
        }
        if (batch.getQuantityOnHand().compareTo(quantity) < 0) {
            throw ApiException.unprocessable("Insufficient medicine stock");
        }
        return batch;
    }

    private void extendWithdrawal(Treatment treatment, Integer milkHours, Integer meatHours) {
        if (milkHours != null) {
            Instant until = treatment.getTreatmentDate().plus(milkHours, ChronoUnit.HOURS);
            if (treatment.getMilkWithdrawalUntil() == null || until.isAfter(treatment.getMilkWithdrawalUntil())) {
                treatment.setMilkWithdrawalUntil(until);
            }
        }
        if (meatHours != null) {
            Instant until = treatment.getTreatmentDate().plus(meatHours, ChronoUnit.HOURS);
            if (treatment.getMeatWithdrawalUntil() == null || until.isAfter(treatment.getMeatWithdrawalUntil())) {
                treatment.setMeatWithdrawalUntil(until);
            }
        }
    }

    private void apply(VeterinaryVisit visit, SaveVisitRequest request) {
        Animal animal = animals.requirePresent(request.animalId(), visit.getFarm().getId());
        visit.setAnimal(animal);
        visit.setVisitAt(request.visitAt() == null ? Instant.now() : request.visitAt());
        visit.setVisitType(request.visitType() == null ? VisitType.EXAMINATION : request.visitType());
        visit.setSymptoms(DomainSupport.trimToNull(request.symptoms()));
        visit.setDiagnosis(DomainSupport.trimToNull(request.diagnosis()));
        visit.setTemperatureC(request.temperatureC());
        visit.setWeightKg(request.weightKg());
        visit.setVetNotes(DomainSupport.trimToNull(request.vetNotes()));
        visit.setFollowUpAt(request.followUpAt());
    }

    private void apply(Treatment treatment, SaveTreatmentRequest request) {
        Animal animal = animals.requirePresent(request.animalId(), treatment.getFarm().getId());
        treatment.setAnimal(animal);
        if (request.veterinaryVisitId() != null) {
            VeterinaryVisit visit = loadVisit(request.veterinaryVisitId());
            if (!visit.getFarm().getId().equals(treatment.getFarm().getId())
                    || !visit.getAnimal().getId().equals(animal.getId())) {
                throw ApiException.unprocessable("Veterinary visit does not match this farm and animal");
            }
            treatment.setVeterinaryVisit(visit);
        } else {
            treatment.setVeterinaryVisit(null);
        }
        treatment.setTreatmentDate(request.treatmentDate() == null ? Instant.now() : request.treatmentDate());
        treatment.setDiagnosis(DomainSupport.trimToNull(request.diagnosis()));
        treatment.setTreatmentPlan(DomainSupport.trimToNull(request.treatmentPlan()));
        if (request.outcome() != null) {
            treatment.setOutcome(request.outcome());
        }
        treatment.setMilkWithdrawalUntil(request.milkWithdrawalUntil());
        treatment.setMeatWithdrawalUntil(request.meatWithdrawalUntil());
    }

    private void recordWeight(VeterinaryVisit visit) {
        AnimalWeightRecord weight = new AnimalWeightRecord();
        weight.setAnimal(visit.getAnimal());
        weight.setFarm(visit.getFarm());
        weight.setMeasuredAt(visit.getVisitAt());
        weight.setWeightKg(visit.getWeightKg());
        weight.setMeasurementMethod("VET_VISIT");
        weight.setRecordedBy(support.actor());
        weights.save(weight);
    }

    private VeterinaryVisit loadVisit(UUID id) {
        VeterinaryVisit visit = visits.findById(id).orElseThrow(() -> ApiException.notFound("Veterinary visit not found"));
        if (!visit.getFarm().getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Veterinary visit not found");
        }
        return visit;
    }

    private Treatment loadTreatment(UUID id) {
        Treatment treatment = treatments.findById(id).orElseThrow(() -> ApiException.notFound("Treatment not found"));
        if (!treatment.getFarm().getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Treatment not found");
        }
        return treatment;
    }

    private VisitResponse toResponse(VeterinaryVisit visit) {
        return new VisitResponse(
                visit.getId(), visit.getFarm().getId(), visit.getAnimal().getId(), visit.getAnimal().getAnimalNumber(),
                visit.getVisitAt(), visit.getVisitType(), visit.getSymptoms(), visit.getDiagnosis(),
                visit.getTemperatureC(), visit.getWeightKg(), visit.getVetNotes(), visit.getFollowUpAt(),
                visit.getCreatedAt(), visit.getUpdatedAt());
    }

    private VaccinationResponse toResponse(Vaccination vaccination) {
        return new VaccinationResponse(
                vaccination.getId(),
                vaccination.getVeterinaryVisit() == null ? null : vaccination.getVeterinaryVisit().getId(),
                vaccination.getFarm().getId(),
                vaccination.getAnimal().getId(),
                vaccination.getAnimal().getAnimalNumber(),
                vaccination.getVaccineName(),
                vaccination.getAdministeredAt(),
                vaccination.getDose(),
                vaccination.getNextDueOn(),
                vaccination.getBatchNumber(),
                vaccination.getNotes(),
                vaccination.getCreatedAt(),
                vaccination.getMedicineProduct() == null ? null : vaccination.getMedicineProduct().getId(),
                vaccination.getMedicineBatch() == null ? null : vaccination.getMedicineBatch().getId(),
                vaccination.getQuantityUsed(),
                vaccination.getMedicineProduct() == null ? null : vaccination.getMedicineProduct().getBaseUnit());
    }

    private TreatmentResponse toResponse(Treatment treatment, boolean withMedications) {
        List<MedicationResponse> rows = withMedications
                ? medications.findByTreatmentIdOrderByCreatedAtAsc(treatment.getId()).stream().map(this::toResponse).toList()
                : null;
        return new TreatmentResponse(
                treatment.getId(),
                treatment.getVeterinaryVisit() == null ? null : treatment.getVeterinaryVisit().getId(),
                treatment.getFarm().getId(),
                treatment.getAnimal().getId(),
                treatment.getAnimal().getAnimalNumber(),
                treatment.getTreatmentDate(),
                treatment.getDiagnosis(),
                treatment.getTreatmentPlan(),
                treatment.getOutcome(),
                treatment.getMilkWithdrawalUntil(),
                treatment.getMeatWithdrawalUntil(),
                rows,
                treatment.getCreatedAt(),
                treatment.getUpdatedAt());
    }

    private MedicationResponse toResponse(TreatmentMedication medication) {
        return new MedicationResponse(
                medication.getId(),
                medication.getTreatment().getId(),
                medication.getMedicineProduct().getId(),
                medication.getMedicineProduct().getName(),
                medication.getBatch() == null ? null : medication.getBatch().getId(),
                medication.getBatch() == null ? null : medication.getBatch().getBatchNumber(),
                medication.getDoseAmount(),
                medication.getDoseUnit(),
                medication.getRoute(),
                medication.getAdministrationsCount(),
                medication.getQuantityUsed(),
                medication.getFrequency(),
                medication.getDurationText(),
                medication.getMilkWithdrawalHours(),
                medication.getMeatWithdrawalHours(),
                medication.getNotes(),
                medication.getCreatedAt());
    }
}
