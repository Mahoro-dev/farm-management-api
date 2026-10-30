package rw.farm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.farm.common.ApiException;
import rw.farm.domain.Enums.RoleCode;
import rw.farm.domain.Farm;
import rw.farm.domain.Organization;
import rw.farm.repo.FarmRepository;
import rw.farm.repo.OrganizationRepository;
import rw.farm.repo.UserFarmAssignmentRepository;
import rw.farm.security.SecurityUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessService {

    public static final RoleCode[] ANIMAL_WRITE = {RoleCode.FARM_MANAGER, RoleCode.VETERINARIAN};
    public static final RoleCode[] MILK_WRITE = {RoleCode.FARM_MANAGER, RoleCode.FARM_WORKER};
    public static final RoleCode[] SHIFT_WRITE = {RoleCode.FARM_MANAGER};
    public static final RoleCode[] CLINICAL_WRITE = {RoleCode.FARM_MANAGER, RoleCode.VETERINARIAN};
    public static final RoleCode[] INVENTORY_WRITE = {RoleCode.INVENTORY_MANAGER, RoleCode.FARM_MANAGER};
    public static final RoleCode[] BREEDING_WRITE = {RoleCode.FARM_MANAGER, RoleCode.VETERINARIAN};
    public static final RoleCode[] WEIGHT_WRITE = {RoleCode.FARM_MANAGER, RoleCode.FARM_WORKER, RoleCode.VETERINARIAN};

    private final FarmRepository farms;
    private final OrganizationRepository organizations;
    private final UserFarmAssignmentRepository assignments;

    public boolean isOrgAdmin() {
        return assignments.countActiveRole(SecurityUtils.userId(), RoleCode.ORG_ADMIN.name()) > 0;
    }

    public void requireOrgAdmin() {
        if (!isOrgAdmin()) {
            throw ApiException.forbidden("Organization administrator access is required");
        }
    }

    public void requireMember() {
        if (isOrgAdmin() || !assignments.activeFarmIds(SecurityUtils.userId()).isEmpty()) {
            return;
        }
        throw ApiException.forbidden("You do not have access to this organization");
    }

    public boolean hasAnyRole(RoleCode... roles) {
        if (isOrgAdmin()) {
            return true;
        }
        return assignments.countAnyRole(SecurityUtils.userId(), names(roles)) > 0;
    }

    public void requireAnyRole(RoleCode... roles) {
        if (!hasAnyRole(roles)) {
            throw ApiException.forbidden("You do not have access to this resource");
        }
    }

    public Organization currentOrganization() {
        return organizations.findById(SecurityUtils.organizationId())
                .orElseThrow(() -> ApiException.notFound("Organization not found"));
    }

    public Farm requireFarmRead(UUID farmId) {
        Farm farm = loadFarm(farmId);
        if (isOrgAdmin() || assignments.countActiveFarmAccess(SecurityUtils.userId(), farmId) > 0) {
            return farm;
        }
        throw ApiException.forbidden("You do not have access to this farm");
    }

    public Farm requireFarmWrite(UUID farmId, RoleCode... roles) {
        Farm farm = loadFarm(farmId);
        if (isOrgAdmin()) {
            return farm;
        }
        if (assignments.countActiveFarmRole(SecurityUtils.userId(), farmId, names(roles)) == 0) {
            throw ApiException.forbidden("You do not have access to change records on this farm");
        }
        return farm;
    }

    public List<UUID> accessibleFarmIds() {
        if (isOrgAdmin()) {
            return List.of();
        }
        return assignments.activeFarmIds(SecurityUtils.userId());
    }

    private Farm loadFarm(UUID farmId) {
        Farm farm = farms.findById(farmId)
                .orElseThrow(() -> ApiException.notFound("Farm not found"));
        if (!farm.getOrganization().getId().equals(SecurityUtils.organizationId())) {
            throw ApiException.notFound("Farm not found");
        }
        return farm;
    }

    private List<String> names(RoleCode... roles) {
        return Arrays.stream(roles).map(Enum::name).toList();
    }
}
