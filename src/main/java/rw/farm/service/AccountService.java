package rw.farm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.farm.common.ApiException;
import rw.farm.common.PageResponse;
import rw.farm.common.Pages;
import rw.farm.domain.Enums.RoleCode;
import rw.farm.domain.Farm;
import rw.farm.domain.Organization;
import rw.farm.domain.Role;
import rw.farm.domain.User;
import rw.farm.domain.UserFarmAssignment;
import rw.farm.dto.AccountDtos.ActiveRequest;
import rw.farm.dto.AccountDtos.AssignmentResponse;
import rw.farm.dto.AccountDtos.AuthResponse;
import rw.farm.dto.AccountDtos.BootstrapRequest;
import rw.farm.dto.AccountDtos.CreateAssignmentRequest;
import rw.farm.dto.AccountDtos.CreateUserRequest;
import rw.farm.dto.AccountDtos.FarmResponse;
import rw.farm.dto.AccountDtos.LoginRequest;
import rw.farm.dto.AccountDtos.NewFarmRequest;
import rw.farm.dto.AccountDtos.OrganizationResponse;
import rw.farm.dto.AccountDtos.PersonResponse;
import rw.farm.dto.AccountDtos.RoleResponse;
import rw.farm.dto.AccountDtos.UpdateFarmRequest;
import rw.farm.dto.AccountDtos.UpdateOrganizationRequest;
import rw.farm.dto.AccountDtos.UpdateUserRequest;
import rw.farm.dto.AccountDtos.UserResponse;
import rw.farm.repo.FarmRepository;
import rw.farm.repo.OrganizationRepository;
import rw.farm.repo.RoleRepository;
import rw.farm.repo.UserFarmAssignmentRepository;
import rw.farm.repo.UserRepository;
import rw.farm.security.JwtService;
import rw.farm.security.SecurityUtils;
import rw.farm.security.UserPrincipal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final OrganizationRepository organizations;
    private final UserRepository users;
    private final FarmRepository farms;
    private final RoleRepository roles;
    private final UserFarmAssignmentRepository assignments;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccessService access;
    private final DomainSupport support;

    @Transactional
    public AuthResponse bootstrap(BootstrapRequest request) {
        Organization organization = new Organization();
        organization.setName(request.organizationName().trim());
        organization.setLegalName(DomainSupport.trimToNull(request.legalName()));
        organization.setRegistrationNumber(DomainSupport.trimToNull(request.registrationNumber()));
        organization.setTimezone(timezoneOrDefault(request.timezone()));
        organizations.save(organization);

        User admin = newUser(
                organization,
                request.admin().fullName(),
                request.admin().email(),
                request.admin().phone(),
                request.admin().password(),
                null);
        users.save(admin);

        Farm farm = newFarm(organization, request.farm(), admin);
        farms.save(farm);
        assignments.save(assignment(admin, farm, RoleCode.ORG_ADMIN, null));
        return tokenFor(admin);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isActive()) {
            throw ApiException.forbidden("This account is disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        user.setLastLoginAt(Instant.now());
        return tokenFor(user);
    }

    public UserResponse me() {
        return toResponse(loadUser(SecurityUtils.userId()), true);
    }

    public OrganizationResponse organization() {
        access.requireMember();
        return toResponse(access.currentOrganization());
    }

    @Transactional
    public OrganizationResponse updateOrganization(UpdateOrganizationRequest request) {
        access.requireOrgAdmin();
        Organization organization = access.currentOrganization();
        organization.setName(request.name().trim());
        organization.setLegalName(DomainSupport.trimToNull(request.legalName()));
        organization.setRegistrationNumber(DomainSupport.trimToNull(request.registrationNumber()));
        organization.setTimezone(request.timezone().trim());
        return toResponse(organization);
    }

    public List<PersonResponse> people() {
        access.requireMember();
        return users.findByOrganizationIdAndActiveTrueOrderByFullNameAsc(SecurityUtils.organizationId()).stream()
                .map(user -> new PersonResponse(user.getId(), user.getFullName()))
                .toList();
    }

    public PageResponse<UserResponse> users(int page, int size) {
        access.requireOrgAdmin();
        Pageable pageable = Pages.of(page, size);
        Page<UserResponse> result = users.findByOrganizationId(SecurityUtils.organizationId(), pageable)
                .map(user -> toResponse(user, false));
        return PageResponse.of(result);
    }

    public UserResponse user(UUID id) {
        access.requireOrgAdmin();
        return toResponse(loadUser(id), true);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        access.requireOrgAdmin();
        return toResponse(newUser(
                access.currentOrganization(),
                request.fullName(),
                request.email(),
                request.phone(),
                request.password(),
                support.actor()), false);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        access.requireOrgAdmin();
        User user = loadUser(id);
        String email = request.email().trim().toLowerCase();
        users.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw ApiException.conflict("A user with this email already exists");
                });
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(DomainSupport.trimToNull(request.phone()));
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setUpdatedBy(support.actor());
        return toResponse(user, true);
    }

    @Transactional
    public UserResponse setActive(UUID id, ActiveRequest request) {
        access.requireOrgAdmin();
        User user = loadUser(id);
        if (!request.active() && isLastOrgAdmin(user)) {
            throw ApiException.conflict("The organization needs at least one active administrator");
        }
        user.setActive(request.active());
        user.setUpdatedBy(support.actor());
        return toResponse(user, true);
    }

    public List<RoleResponse> roles() {
        access.requireMember();
        return roles.findAll().stream()
                .map(role -> new RoleResponse(role.getCode(), role.getName(), role.getDescription()))
                .toList();
    }

    public PageResponse<FarmResponse> farms(int page, int size) {
        access.requireMember();
        if (access.isOrgAdmin()) {
            return PageResponse.of(farms.findByOrganizationId(SecurityUtils.organizationId(), Pages.of(page, size))
                    .map(this::toResponse));
        }
        List<UUID> farmIds = access.accessibleFarmIds();
        List<FarmResponse> visible = farms.findAllById(farmIds).stream()
                .filter(farm -> farm.getOrganization().getId().equals(SecurityUtils.organizationId()))
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(visible, 0, visible.size(), visible.size(), visible.isEmpty() ? 0 : 1);
    }

    public FarmResponse farm(UUID id) {
        return toResponse(access.requireFarmRead(id));
    }

    @Transactional
    public FarmResponse createFarm(NewFarmRequest request) {
        access.requireOrgAdmin();
        return toResponse(newFarm(access.currentOrganization(), request, support.actor()));
    }

    @Transactional
    public FarmResponse updateFarm(UUID id, UpdateFarmRequest request) {
        access.requireOrgAdmin();
        Farm farm = access.requireFarmRead(id);
        farm.setName(request.name().trim());
        farm.setFarmCode(request.farmCode().trim());
        farm.setLocationText(DomainSupport.trimToNull(request.locationText()));
        farm.setDistrict(DomainSupport.trimToNull(request.district()));
        farm.setProvince(DomainSupport.trimToNull(request.province()));
        farm.setCountryCode(request.countryCode().trim().toUpperCase());
        farm.setTimezone(request.timezone().trim());
        farm.setActive(request.active());
        farm.setUpdatedBy(support.actor());
        return toResponse(farm);
    }

    public List<AssignmentResponse> assignments(UUID farmId) {
        access.requireFarmRead(farmId);
        return assignments.findByFarmIdOrderByAssignedAtDesc(farmId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AssignmentResponse assign(UUID farmId, CreateAssignmentRequest request) {
        access.requireOrgAdmin();
        Farm farm = access.requireFarmRead(farmId);
        User user = loadUser(request.userId());
        Role role = roles.findById(request.role().name())
                .orElseThrow(() -> ApiException.badRequest("Unknown role"));
        UserFarmAssignment assignment = assignment(user, farm, request.role(), support.actor());
        assignment.setRole(role);
        return toResponse(assignments.save(assignment));
    }

    @Transactional
    public AssignmentResponse endAssignment(UUID id) {
        access.requireOrgAdmin();
        UserFarmAssignment assignment = assignments.findById(id)
                .orElseThrow(() -> ApiException.notFound("Assignment not found"));
        if (!assignment.getFarm().getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Assignment not found");
        }
        if (assignment.getEndedAt() != null) {
            throw ApiException.conflict("This assignment has already ended");
        }
        if (RoleCode.ORG_ADMIN.name().equals(assignment.getRole().getCode())
                && assignments.countOtherAdminAssignments(assignment.getUser().getId(), assignment.getId()) == 0
                && isLastOrgAdmin(assignment.getUser())) {
            throw ApiException.conflict("The organization needs at least one active administrator");
        }
        assignment.setEndedAt(Instant.now());
        assignment.setEndedBy(support.actor());
        return toResponse(assignment);
    }

    private AuthResponse tokenFor(User user) {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getOrganization().getId(), user.getEmail(), user.getPasswordHash(), user.isActive());
        JwtService.IssuedToken issued = jwtService.issue(principal);
        return new AuthResponse(issued.token(), issued.expiresAt(), toResponse(user, true));
    }

    private User newUser(
            Organization organization, String fullName, String email, String phone, String password, User actor) {
        String normalized = email.trim().toLowerCase();
        users.findByEmailIgnoreCase(normalized).ifPresent(existing -> {
            throw ApiException.conflict("A user with this email already exists");
        });
        User user = new User();
        user.setOrganization(organization);
        user.setFullName(fullName.trim());
        user.setEmail(normalized);
        user.setPhone(DomainSupport.trimToNull(phone));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setActive(true);
        user.setCreatedBy(actor);
        user.setUpdatedBy(actor);
        return users.save(user);
    }

    private Farm newFarm(Organization organization, NewFarmRequest request, User actor) {
        Farm farm = new Farm();
        farm.setOrganization(organization);
        farm.setName(request.name().trim());
        farm.setFarmCode(request.farmCode().trim());
        farm.setLocationText(DomainSupport.trimToNull(request.locationText()));
        farm.setDistrict(DomainSupport.trimToNull(request.district()));
        farm.setProvince(DomainSupport.trimToNull(request.province()));
        String country = DomainSupport.trimToNull(request.countryCode());
        farm.setCountryCode(country == null ? "RW" : country.toUpperCase());
        farm.setTimezone(timezoneOrDefault(request.timezone()));
        farm.setActive(true);
        farm.setCreatedBy(actor);
        farm.setUpdatedBy(actor);
        return farms.save(farm);
    }

    private UserFarmAssignment assignment(User user, Farm farm, RoleCode roleCode, User actor) {
        Role role = roles.findById(roleCode.name())
                .orElseThrow(() -> ApiException.unprocessable("Role " + roleCode + " is not configured"));
        UserFarmAssignment assignment = new UserFarmAssignment();
        assignment.setUser(user);
        assignment.setFarm(farm);
        assignment.setRole(role);
        assignment.setAssignedBy(actor);
        return assignment;
    }

    private User loadUser(UUID id) {
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if (!user.getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("User not found");
        }
        return user;
    }

    private boolean isLastOrgAdmin(User user) {
        boolean admin = assignments.countActiveRole(user.getId(), RoleCode.ORG_ADMIN.name()) > 0;
        return admin && user.isActive() && assignments.countActiveOrgAdmins() <= 1;
    }

    private UserResponse toResponse(User user, boolean withAssignments) {
        List<AssignmentResponse> active = withAssignments
                ? assignments.findByUserIdAndEndedAtIsNull(user.getId()).stream().map(this::toResponse).toList()
                : null;
        return new UserResponse(
                user.getId(),
                user.getOrganization().getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                active);
    }

    private FarmResponse toResponse(Farm farm) {
        return new FarmResponse(
                farm.getId(),
                farm.getOrganization().getId(),
                farm.getName(),
                farm.getFarmCode(),
                farm.getLocationText(),
                farm.getDistrict(),
                farm.getProvince(),
                farm.getCountryCode(),
                farm.getTimezone(),
                farm.isActive(),
                farm.getCreatedAt(),
                farm.getUpdatedAt());
    }

    private AssignmentResponse toResponse(UserFarmAssignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getUser().getId(),
                assignment.getUser().getFullName(),
                assignment.getFarm().getId(),
                assignment.getFarm().getName(),
                assignment.getRole().getCode(),
                assignment.getRole().getName(),
                assignment.getAssignedAt(),
                assignment.getEndedAt());
    }

    private OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getLegalName(),
                organization.getRegistrationNumber(),
                organization.getTimezone(),
                organization.getCreatedAt(),
                organization.getUpdatedAt());
    }

    private String timezoneOrDefault(String timezone) {
        String value = DomainSupport.trimToNull(timezone);
        return value == null ? "Africa/Kigali" : value;
    }
}
