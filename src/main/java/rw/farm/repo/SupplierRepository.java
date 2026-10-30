package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.Supplier;

import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    Page<Supplier> findByOrganizationIdOrderByNameAsc(UUID organizationId, Pageable pageable);

    Page<Supplier> findByOrganizationIdAndNameContainingIgnoreCaseOrderByNameAsc(
            UUID organizationId, String name, Pageable pageable);

    Optional<Supplier> findByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
