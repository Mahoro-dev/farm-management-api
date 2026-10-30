package rw.farm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import rw.farm.domain.Enums.AcquisitionType;
import rw.farm.domain.Enums.AnimalStatus;
import rw.farm.domain.Enums.IdentifierType;
import rw.farm.domain.Enums.Sex;
import rw.farm.domain.Enums.TransferReason;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AnimalDtos {

    private AnimalDtos() {
    }

    public record CreateAnimalRequest(
            @NotNull UUID farmId,
            @NotNull Integer speciesId,
            UUID breedId,
            @NotBlank @Size(max = 60) String animalNumber,
            @Size(max = 100) String name,
            @NotNull Sex sex,
            LocalDate dateOfBirth,
            boolean birthDateEstimated,
            String colorMarkings,
            @NotNull AcquisitionType acquisitionType,
            LocalDate acquiredOn,
            UUID damId,
            UUID sireId,
            UUID semenStrawId,
            String notes,
            @Valid IdentifierRequest identifier
    ) {
    }

    public record UpdateAnimalRequest(
            @NotNull Integer speciesId,
            UUID breedId,
            @NotBlank @Size(max = 60) String animalNumber,
            @Size(max = 100) String name,
            @NotNull Sex sex,
            LocalDate dateOfBirth,
            boolean birthDateEstimated,
            String colorMarkings,
            @NotNull AcquisitionType acquisitionType,
            LocalDate acquiredOn,
            UUID damId,
            UUID sireId,
            UUID semenStrawId,
            String notes
    ) {
    }

    public record AnimalResponse(
            UUID id,
            UUID organizationId,
            Short speciesId,
            String speciesCode,
            UUID breedId,
            String breedName,
            String animalNumber,
            String name,
            Sex sex,
            LocalDate dateOfBirth,
            boolean birthDateEstimated,
            String colorMarkings,
            AcquisitionType acquisitionType,
            LocalDate acquiredOn,
            UUID damId,
            String damNumber,
            UUID sireId,
            String sireNumber,
            String artificialSireCode,
            UUID semenStrawId,
            AnimalStatus status,
            LocalDate statusChangedOn,
            String notes,
            UUID currentFarmId,
            String currentFarmName,
            String primaryIdentifier,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record TagResponse(String type, String value) {
    }

    public record HerdTagResponse(
            UUID id,
            String animalNumber,
            String name,
            Sex sex,
            AnimalStatus status,
            UUID currentFarmId,
            List<TagResponse> tags
    ) {
    }

    public record ChangeStatusRequest(
            @NotNull AnimalStatus status,
            @NotNull LocalDate statusChangedOn,
            String notes
    ) {
    }

    public record TransferRequest(
            @NotNull UUID toFarmId,
            @NotNull LocalDate transferOn,
            String notes
    ) {
    }

    public record IdentifierRequest(
            @NotNull IdentifierType identifierType,
            @NotBlank @Size(max = 120) String identifierValue,
            @Size(max = 120) String issuingAuthority,
            LocalDate issuedAt,
            boolean primary,
            String notes
    ) {
    }

    public record RetireIdentifierRequest(@NotNull LocalDate retiredAt) {
    }

    public record IdentifierResponse(
            UUID id,
            UUID animalId,
            IdentifierType identifierType,
            String identifierValue,
            String issuingAuthority,
            LocalDate issuedAt,
            LocalDate retiredAt,
            boolean primary,
            String notes,
            Instant createdAt
    ) {
    }

    public record FarmHistoryResponse(
            UUID id,
            UUID animalId,
            UUID farmId,
            String farmName,
            LocalDate joinedOn,
            LocalDate leftOn,
            TransferReason transferReason,
            String notes,
            Instant recordedAt
    ) {
    }

    public record SpeciesRequest(
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 80) String name
    ) {
    }

    public record SpeciesResponse(Short id, String code, String name, boolean active) {
    }

    public record BreedRequest(
            @NotBlank @Size(max = 100) String name,
            String description,
            Boolean active
    ) {
    }

    public record BreedResponse(UUID id, String name, String description, boolean active) {
    }

    public record SemenStrawRequest(
            @NotBlank @Size(max = 100) String code,
            String description,
            Boolean active
    ) {
    }

    public record SemenStrawResponse(UUID id, String code, String description, boolean active) {
    }
}
