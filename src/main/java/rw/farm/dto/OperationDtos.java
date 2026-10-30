package rw.farm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import rw.farm.domain.Enums.AdministrationRoute;
import rw.farm.domain.Enums.AnimalEventType;
import rw.farm.domain.Enums.AnimalStatus;
import rw.farm.domain.Enums.BreedingMethod;
import rw.farm.domain.Enums.BreedingOutcome;
import rw.farm.domain.Enums.CalvingOutcome;
import rw.farm.domain.Enums.MedicineForm;
import rw.farm.domain.Enums.MedicineUnit;
import rw.farm.domain.Enums.PregnancyStatus;
import rw.farm.domain.Enums.Sex;
import rw.farm.domain.Enums.StockMovementType;
import rw.farm.domain.Enums.SupplierKind;
import rw.farm.domain.Enums.TreatmentOutcome;
import rw.farm.domain.Enums.VisitType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class OperationDtos {

    private OperationDtos() {
    }

    public record SaveShiftRequest(
            @NotBlank @Size(max = 60) String name,
            LocalTime startTime,
            LocalTime endTime,
            @NotNull @Min(1) Integer displayOrder,
            Boolean active
    ) {
    }

    public record ShiftResponse(
            UUID id,
            UUID farmId,
            String name,
            LocalTime startTime,
            LocalTime endTime,
            short displayOrder,
            boolean active
    ) {
    }

    public record LactationStageResponse(
            String code,
            String name,
            String meaning,
            int displayOrder
    ) {
    }

    public record SaveMilkRequest(
            @NotNull UUID animalId,
            @NotNull UUID shiftId,
            LocalDate milkingDate,
            @NotNull @DecimalMin("0.0") BigDecimal quantityLiters,
            @NotBlank @Size(max = 40) String lactationStage,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal fatPercent,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal proteinPercent,
            @Min(0) Integer somaticCellCount,
            String notes
    ) {
    }

    public record MilkResponse(
            UUID id,
            UUID farmId,
            UUID animalId,
            String animalNumber,
            UUID shiftId,
            String shiftName,
            LocalDate milkingDate,
            BigDecimal quantityLiters,
            String lactationStage,
            String lactationStageName,
            BigDecimal fatPercent,
            BigDecimal proteinPercent,
            Integer somaticCellCount,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record SaveVisitRequest(
            @NotNull UUID animalId,
            Instant visitAt,
            VisitType visitType,
            String symptoms,
            String diagnosis,
            @DecimalMin("25.0") @DecimalMax("45.0") BigDecimal temperatureC,
            @DecimalMin("0.01") BigDecimal weightKg,
            String vetNotes,
            Instant followUpAt
    ) {
    }

    public record VisitResponse(
            UUID id,
            UUID farmId,
            UUID animalId,
            String animalNumber,
            Instant visitAt,
            VisitType visitType,
            String symptoms,
            String diagnosis,
            BigDecimal temperatureC,
            BigDecimal weightKg,
            String vetNotes,
            Instant followUpAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CreateVaccinationRequest(
            UUID veterinaryVisitId,
            @NotNull UUID animalId,
            @NotNull UUID medicineProductId,
            @NotNull UUID medicineBatchId,
            @NotNull @DecimalMin("0.001") BigDecimal quantityUsed,
            Instant administeredAt,
            LocalDate nextDueOn,
            String notes
    ) {
    }

    public record VaccinationResponse(
            UUID id,
            UUID veterinaryVisitId,
            UUID farmId,
            UUID animalId,
            String animalNumber,
            String vaccineName,
            Instant administeredAt,
            String dose,
            LocalDate nextDueOn,
            String batchNumber,
            String notes,
            Instant createdAt,
            UUID medicineProductId,
            UUID medicineBatchId,
            BigDecimal quantityUsed,
            MedicineUnit baseUnit
    ) {
    }

    public record SaveTreatmentRequest(
            UUID veterinaryVisitId,
            @NotNull UUID animalId,
            Instant treatmentDate,
            String diagnosis,
            String treatmentPlan,
            TreatmentOutcome outcome,
            Instant milkWithdrawalUntil,
            Instant meatWithdrawalUntil
    ) {
    }

    public record TreatmentResponse(
            UUID id,
            UUID veterinaryVisitId,
            UUID farmId,
            UUID animalId,
            String animalNumber,
            Instant treatmentDate,
            String diagnosis,
            String treatmentPlan,
            TreatmentOutcome outcome,
            Instant milkWithdrawalUntil,
            Instant meatWithdrawalUntil,
            List<MedicationResponse> medications,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record AddMedicationRequest(
            @NotNull UUID medicineProductId,
            @NotNull(message = "Choose the batch. Medicine given to an animal is taken from the store") UUID batchId,
            @NotNull @DecimalMin("0.001") BigDecimal doseAmount,
            @NotBlank @Size(max = 30) String doseUnit,
            @NotNull AdministrationRoute route,
            @NotNull @Min(1) Integer administrationsCount,
            @NotNull @DecimalMin("0.001") BigDecimal quantityUsed,
            @Size(max = 100) String frequency,
            @Size(max = 100) String durationText,
            @Min(0) Integer milkWithdrawalHours,
            @Min(0) Integer meatWithdrawalHours,
            String notes
    ) {
    }

    public record MedicationResponse(
            UUID id,
            UUID treatmentId,
            UUID medicineProductId,
            String medicineName,
            UUID batchId,
            String batchNumber,
            BigDecimal doseAmount,
            String doseUnit,
            AdministrationRoute route,
            short administrationsCount,
            BigDecimal quantityUsed,
            String frequency,
            String durationText,
            Integer milkWithdrawalHours,
            Integer meatWithdrawalHours,
            String notes,
            Instant createdAt
    ) {
    }

    public record SaveProductRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 160) String activeIngredient,
            @NotNull MedicineForm form,
            @Size(max = 100) String formulation,
            @NotNull MedicineUnit baseUnit,
            String description,
            boolean requiresPrescription,
            Boolean active
    ) {
    }

    public record ProductResponse(
            UUID id,
            UUID organizationId,
            String name,
            String activeIngredient,
            MedicineForm form,
            String formulation,
            MedicineUnit baseUnit,
            String description,
            boolean requiresPrescription,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record SaveSupplierRequest(
            @NotBlank @Size(max = 160) String name,
            @NotNull SupplierKind kind,
            @Size(max = 40) String phone,
            String notes,
            Boolean active
    ) {
    }

    public record SupplierResponse(
            UUID id,
            String name,
            SupplierKind kind,
            String phone,
            String notes,
            boolean active
    ) {
    }

    /**
     * supplierId is a supplier or pharmacy saved for the organization.
     * batchNumber may be omitted for a pharmacy purchase; a receipt code is assigned.
     * initialQuantity is the amount received, in the product unit. A pharmacy purchase can be 1 or 2.
     */
    public record CreateBatchRequest(
            @NotNull UUID medicineProductId,
            @Size(max = 100) String batchNumber,
            @NotNull LocalDate expiryDate,
            LocalDate receivedOn,
            @NotNull UUID supplierId,
            @DecimalMin("0.0") BigDecimal unitCost,
            @DecimalMin("0.0") BigDecimal initialQuantity
    ) {
    }

    public record UpdateBatchRequest(
            @NotNull LocalDate expiryDate,
            @NotNull LocalDate receivedOn,
            UUID supplierId,
            @DecimalMin("0.0") BigDecimal unitCost
    ) {
    }

    /** unitCost is the price of one base unit, in RWF. */
    public record BatchResponse(
            UUID id,
            UUID farmId,
            UUID medicineProductId,
            String medicineName,
            MedicineForm form,
            MedicineUnit baseUnit,
            String batchNumber,
            LocalDate expiryDate,
            LocalDate receivedOn,
            UUID supplierId,
            String supplierName,
            BigDecimal unitCost,
            BigDecimal quantityOnHand,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    /**
     * quantity is always a positive amount, except for ADJUSTMENT where it is a signed RWF-free stock delta.
     * PURCHASE, RETURN and TRANSFER_IN add stock. EXPIRED and TRANSFER_OUT remove stock.
     * TREATMENT_USE is created from a treatment medication, not this endpoint.
     */
    public record PostMovementRequest(
            @NotNull StockMovementType movementType,
            @NotNull BigDecimal quantity,
            @Size(max = 160) String referenceText,
            String notes
    ) {
    }

    public record MovementResponse(
            UUID id,
            UUID batchId,
            StockMovementType movementType,
            BigDecimal quantityChange,
            Instant movementAt,
            UUID treatmentMedicationId,
            String referenceText,
            String notes,
            BigDecimal quantityOnHand,
            Instant createdAt
    ) {
    }

    public record CreateBreedingRequest(
            @NotNull UUID cowId,
            UUID sireId,
            @NotNull Instant breedingAt,
            @NotNull BreedingMethod breedingMethod,
            BreedingOutcome outcome,
            UUID technicianId,
            UUID semenStrawId,
            String ancestryOverrideReason,
            String notes
    ) {
    }

    public record UpdateBreedingRequest(
            UUID sireId,
            @NotNull Instant breedingAt,
            @NotNull BreedingMethod breedingMethod,
            @NotNull BreedingOutcome outcome,
            UUID technicianId,
            UUID semenStrawId,
            String ancestryOverrideReason,
            String notes
    ) {
    }

    public record BreedingResponse(
            UUID id,
            UUID farmId,
            UUID cowId,
            String cowNumber,
            UUID sireId,
            String sireNumber,
            Instant breedingAt,
            BreedingMethod breedingMethod,
            BreedingOutcome outcome,
            String technicianName,
            UUID technicianId,
            String semenBatchNumber,
            UUID semenStrawId,
            UUID ancestryReviewedBy,
            String ancestryOverrideReason,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record SavePregnancyRequest(
            UUID breedingRecordId,
            @NotNull UUID cowId,
            Instant confirmedAt,
            @Size(max = 40) String confirmationMethod,
            LocalDate expectedCalvingOn,
            PregnancyStatus status,
            String notes
    ) {
    }

    public record PregnancyResponse(
            UUID id,
            UUID breedingRecordId,
            UUID farmId,
            UUID cowId,
            String cowNumber,
            Instant confirmedAt,
            String confirmationMethod,
            LocalDate expectedCalvingOn,
            PregnancyStatus status,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CreateCalvingRequest(
            UUID pregnancyRecordId,
            @NotNull UUID damId,
            UUID sireId,
            UUID semenStrawId,
            UUID calfId,
            @Valid CreateCalfRequest createCalf,
            @NotNull Instant calvedAt,
            @NotNull CalvingOutcome outcome,
            boolean assistanceRequired,
            @NotNull @Min(1) Integer numberOfCalves,
            String complications
    ) {
    }

    public record CreateCalfRequest(
            @NotBlank @Size(max = 60) String animalNumber,
            @Size(max = 100) String name,
            @NotNull Sex sex,
            UUID breedId,
            String colorMarkings
    ) {
    }

    public record CalvingResponse(
            UUID id,
            UUID pregnancyRecordId,
            UUID farmId,
            UUID damId,
            String damNumber,
            UUID sireId,
            String sireNumber,
            String artificialSireCode,
            UUID semenStrawId,
            UUID calfId,
            String calfNumber,
            Instant calvedAt,
            CalvingOutcome outcome,
            boolean assistanceRequired,
            short numberOfCalves,
            String complications,
            Instant createdAt
    ) {
    }

    public record CreateWeightRequest(
            @NotNull UUID farmId,
            Instant measuredAt,
            @NotNull @DecimalMin("0.01") BigDecimal weightKg,
            @Size(max = 30) String measurementMethod,
            String notes
    ) {
    }

    public record WeightResponse(
            UUID id,
            UUID animalId,
            UUID farmId,
            Instant measuredAt,
            BigDecimal weightKg,
            String measurementMethod,
            String notes,
            Instant createdAt
    ) {
    }

    public record CreateEventRequest(
            UUID farmId,
            @NotNull AnimalEventType eventType,
            Instant eventAt,
            String details
    ) {
    }

    public record EventResponse(
            UUID id,
            UUID animalId,
            UUID farmId,
            AnimalEventType eventType,
            Instant eventAt,
            String details,
            Instant createdAt
    ) {
    }

    public record DailyMilkResponse(
            UUID farmId,
            LocalDate milkingDate,
            BigDecimal totalLiters,
            long animalsMilked
    ) {
    }

    public record ExpiryWatchResponse(
            UUID batchId,
            UUID farmId,
            String medicineName,
            String batchNumber,
            LocalDate expiryDate,
            BigDecimal quantityOnHand,
            BigDecimal unitCost
    ) {
    }

    public record HerdAnimalResponse(
            UUID animalId,
            String animalNumber,
            String name,
            Sex sex,
            AnimalStatus status,
            LocalDate joinedOn,
            String breedName
    ) {
    }

    public record WithdrawalResponse(
            UUID treatmentId,
            UUID animalId,
            String animalNumber,
            Instant milkWithdrawalUntil,
            Instant meatWithdrawalUntil,
            TreatmentOutcome outcome
    ) {
    }

    public record AuditResponse(
            Long id,
            String tableName,
            UUID recordId,
            String action,
            Instant changedAt,
            UUID changedBy,
            String oldData,
            String newData
    ) {
    }
}
