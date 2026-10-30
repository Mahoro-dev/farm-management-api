package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.BreedingRecord;

import java.util.UUID;

public interface BreedingRecordRepository extends JpaRepository<BreedingRecord, UUID> {
    Page<BreedingRecord> findByFarmIdOrderByBreedingAtDesc(UUID farmId, Pageable pageable);

    Page<BreedingRecord> findByCowIdOrderByBreedingAtDesc(UUID cowId, Pageable pageable);
}
