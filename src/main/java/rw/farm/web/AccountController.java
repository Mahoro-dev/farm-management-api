package rw.farm.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.farm.common.PageResponse;
import rw.farm.dto.AccountDtos.ActiveRequest;
import rw.farm.dto.AccountDtos.AssignmentResponse;
import rw.farm.dto.AccountDtos.CreateAssignmentRequest;
import rw.farm.dto.AccountDtos.CreateUserRequest;
import rw.farm.dto.AccountDtos.FarmResponse;
import rw.farm.dto.AccountDtos.NewFarmRequest;
import rw.farm.dto.AccountDtos.OrganizationResponse;
import rw.farm.dto.AccountDtos.PersonResponse;
import rw.farm.dto.AccountDtos.RoleResponse;
import rw.farm.dto.AccountDtos.UpdateFarmRequest;
import rw.farm.dto.AccountDtos.UpdateOrganizationRequest;
import rw.farm.dto.AccountDtos.UpdateUserRequest;
import rw.farm.dto.AccountDtos.UserResponse;
import rw.farm.service.AccountService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Organization")
public class AccountController {

    private final AccountService accounts;

    @GetMapping("/organization")
    public OrganizationResponse organization() {
        return accounts.organization();
    }

    @PutMapping("/organization")
    public OrganizationResponse updateOrganization(@Valid @RequestBody UpdateOrganizationRequest request) {
        return accounts.updateOrganization(request);
    }

    @GetMapping("/roles")
    public List<RoleResponse> roles() {
        return accounts.roles();
    }

    @GetMapping("/people")
    public List<PersonResponse> people() {
        return accounts.people();
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return accounts.users(page, size);
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse body = accounts.createUser(request);
        return ResponseEntity.created(URI.create("/api/users/" + body.id())).body(body);
    }

    @GetMapping("/users/{id}")
    public UserResponse user(@PathVariable UUID id) {
        return accounts.user(id);
    }

    @PutMapping("/users/{id}")
    public UserResponse updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return accounts.updateUser(id, request);
    }

    @PatchMapping("/users/{id}/active")
    public UserResponse setActive(@PathVariable UUID id, @Valid @RequestBody ActiveRequest request) {
        return accounts.setActive(id, request);
    }

    @GetMapping("/farms")
    public PageResponse<FarmResponse> farms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return accounts.farms(page, size);
    }

    @PostMapping("/farms")
    public ResponseEntity<FarmResponse> createFarm(@Valid @RequestBody NewFarmRequest request) {
        FarmResponse body = accounts.createFarm(request);
        return ResponseEntity.created(URI.create("/api/farms/" + body.id())).body(body);
    }

    @GetMapping("/farms/{id}")
    public FarmResponse farm(@PathVariable UUID id) {
        return accounts.farm(id);
    }

    @PutMapping("/farms/{id}")
    public FarmResponse updateFarm(@PathVariable UUID id, @Valid @RequestBody UpdateFarmRequest request) {
        return accounts.updateFarm(id, request);
    }

    @GetMapping("/farms/{farmId}/assignments")
    public List<AssignmentResponse> assignments(@PathVariable UUID farmId) {
        return accounts.assignments(farmId);
    }

    @PostMapping("/farms/{farmId}/assignments")
    public ResponseEntity<AssignmentResponse> assign(
            @PathVariable UUID farmId, @Valid @RequestBody CreateAssignmentRequest request) {
        AssignmentResponse body = accounts.assign(farmId, request);
        return ResponseEntity.created(URI.create("/api/assignments/" + body.id())).body(body);
    }

    @PostMapping("/assignments/{id}/end")
    public AssignmentResponse endAssignment(@PathVariable UUID id) {
        return accounts.endAssignment(id);
    }
}
