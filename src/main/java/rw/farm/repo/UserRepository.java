package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.farm.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);

    @Query("select u from User u join fetch u.organization where u.id = :id")
    Optional<User> findWithOrganizationById(@Param("id") UUID id);

    Page<User> findByOrganizationId(UUID organizationId, Pageable pageable);

    List<User> findByOrganizationIdAndActiveTrueOrderByFullNameAsc(UUID organizationId);
}
