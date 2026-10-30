package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.SemenStraw;

import java.util.Optional;
import java.util.UUID;

public interface SemenStrawRepository extends JpaRepository<SemenStraw, UUID> {
    Page<SemenStraw> findByOrganizationId(UUID organizationId, Pageable pageable);

    Page<SemenStraw> findByOrganizationIdAndCodeContainingIgnoreCase(UUID organizationId, String code, Pageable pageable);

    Optional<SemenStraw> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}
