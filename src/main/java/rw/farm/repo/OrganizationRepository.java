package rw.farm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.Organization;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findFirstByOrderByCreatedAtAsc();
}
