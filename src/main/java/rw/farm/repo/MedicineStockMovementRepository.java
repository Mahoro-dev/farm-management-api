package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.MedicineStockMovement;

import java.util.UUID;

public interface MedicineStockMovementRepository extends JpaRepository<MedicineStockMovement, UUID> {
    Page<MedicineStockMovement> findByBatchIdOrderByMovementAtDesc(UUID batchId, Pageable pageable);
}
