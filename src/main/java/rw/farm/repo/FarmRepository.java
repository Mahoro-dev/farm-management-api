package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.Farm;

import java.util.UUID;

public interface FarmRepository extends JpaRepository<Farm, UUID> {
    Page<Farm> findByOrganizationId(UUID organizationId, Pageable pageable);
}
