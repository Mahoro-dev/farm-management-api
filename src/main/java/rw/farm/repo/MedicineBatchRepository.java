package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.MedicineBatch;

import java.util.UUID;

public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, UUID> {
    Page<MedicineBatch> findByFarmIdOrderByExpiryDateAsc(UUID farmId, Pageable pageable);

    boolean existsByFarmIdAndMedicineProductIdAndBatchNumber(UUID farmId, UUID medicineProductId, String batchNumber);
}
