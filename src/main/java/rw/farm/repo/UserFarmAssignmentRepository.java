package rw.farm.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.farm.domain.UserFarmAssignment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserFarmAssignmentRepository extends JpaRepository<UserFarmAssignment, UUID> {

    @Query("""
            select count(a) from UserFarmAssignment a
            where a.user.id = :userId and a.endedAt is null and a.role.code = :role
            """)
    long countActiveRole(@Param("userId") UUID userId, @Param("role") String role);

    @Query("""
            select count(a) from UserFarmAssignment a
            where a.user.id = :userId and a.farm.id = :farmId and a.endedAt is null
              and a.role.code in :roles
            """)
    long countActiveFarmRole(
            @Param("userId") UUID userId,
            @Param("farmId") UUID farmId,
            @Param("roles") Collection<String> roles);

    @Query("""
            select count(a) from UserFarmAssignment a
            where a.user.id = :userId and a.farm.id = :farmId and a.endedAt is null
            """)
    long countActiveFarmAccess(@Param("userId") UUID userId, @Param("farmId") UUID farmId);

    @Query("""
            select distinct a.farm.id from UserFarmAssignment a
            where a.user.id = :userId and a.endedAt is null
            """)
    List<UUID> activeFarmIds(@Param("userId") UUID userId);

    @Query("""
            select count(a) from UserFarmAssignment a
            where a.user.id = :userId and a.endedAt is null and a.role.code in :roles
            """)
    long countAnyRole(@Param("userId") UUID userId, @Param("roles") Collection<String> roles);

    @EntityGraph(attributePaths = {"farm", "role"})
    List<UserFarmAssignment> findByUserIdAndEndedAtIsNull(UUID userId);

    @EntityGraph(attributePaths = {"user", "farm", "role"})
    List<UserFarmAssignment> findByFarmIdOrderByAssignedAtDesc(UUID farmId);

    @Query("""
            select count(distinct a.user.id) from UserFarmAssignment a
            where a.endedAt is null and a.role.code = 'ORG_ADMIN' and a.user.active = true
            """)
    long countActiveOrgAdmins();

    @Query("""
            select count(a) from UserFarmAssignment a
            where a.user.id = :userId and a.id <> :assignmentId
              and a.endedAt is null and a.role.code = 'ORG_ADMIN'
            """)
    long countOtherAdminAssignments(@Param("userId") UUID userId, @Param("assignmentId") UUID assignmentId);
}
