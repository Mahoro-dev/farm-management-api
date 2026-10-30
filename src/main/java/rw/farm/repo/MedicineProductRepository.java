package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.MedicineProduct;

import java.util.UUID;

public interface MedicineProductRepository extends JpaRepository<MedicineProduct, UUID> {
    Page<MedicineProduct> findByOrganizationIdOrderByNameAsc(UUID organizationId, Pageable pageable);
}
