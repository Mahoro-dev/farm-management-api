package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.AnimalWeightRecord;

import java.util.UUID;

public interface AnimalWeightRecordRepository extends JpaRepository<AnimalWeightRecord, UUID> {
    Page<AnimalWeightRecord> findByAnimalIdOrderByMeasuredAtDesc(UUID animalId, Pageable pageable);
}
