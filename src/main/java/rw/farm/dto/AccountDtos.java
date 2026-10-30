package rw.farm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import rw.farm.domain.Enums.RoleCode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record BootstrapRequest(
            @NotBlank @Size(max = 160) String organizationName,
            @Size(max = 200) String legalName,
            @Size(max = 100) String registrationNumber,
            @Size(max = 80) String timezone,
            @NotNull @Valid AdminRequest admin,
            @NotNull @Valid NewFarmRequest farm
    ) {
    }

    public record AdminRequest(
            @NotBlank @Size(max = 160) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @Size(max = 40) String phone,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record AuthResponse(String token, Instant expiresAt, UserResponse user) {
    }

    public record PersonResponse(UUID id, String fullName) {
    }

    public record UserResponse(
            UUID id,
            UUID organizationId,
            String fullName,
            String email,
            String phone,
            boolean active,
            Instant lastLoginAt,
            Instant createdAt,
            List<AssignmentResponse> assignments
    ) {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 160) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @Size(max = 40) String phone,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank @Size(max = 160) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @Size(max = 40) String phone,
            @Size(min = 8, max = 72) String password
    ) {
    }

    public record ActiveRequest(@NotNull Boolean active) {
    }

    public record OrganizationResponse(
            UUID id,
            String name,
            String legalName,
            String registrationNumber,
            String timezone,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record UpdateOrganizationRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 200) String legalName,
            @Size(max = 100) String registrationNumber,
            @NotBlank @Size(max = 80) String timezone
    ) {
    }

    public record NewFarmRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 40) String farmCode,
            @Size(max = 255) String locationText,
            @Size(max = 100) String district,
            @Size(max = 100) String province,
            @Size(min = 2, max = 2) String countryCode,
            @Size(max = 80) String timezone
    ) {
    }

    public record UpdateFarmRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 40) String farmCode,
            @Size(max = 255) String locationText,
            @Size(max = 100) String district,
            @Size(max = 100) String province,
            @NotBlank @Size(min = 2, max = 2) String countryCode,
            @NotBlank @Size(max = 80) String timezone,
            @NotNull Boolean active
    ) {
    }

    public record FarmResponse(
            UUID id,
            UUID organizationId,
            String name,
            String farmCode,
            String locationText,
            String district,
            String province,
            String countryCode,
            String timezone,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CreateAssignmentRequest(
            @NotNull UUID userId,
            @NotNull RoleCode role
    ) {
    }

    public record AssignmentResponse(
            UUID id,
            UUID userId,
            String userName,
            UUID farmId,
            String farmName,
            String roleCode,
            String roleName,
            Instant assignedAt,
            Instant endedAt
    ) {
    }

    public record RoleResponse(String code, String name, String description) {
    }
}
