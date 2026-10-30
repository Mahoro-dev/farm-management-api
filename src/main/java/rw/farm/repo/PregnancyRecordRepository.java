package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.PregnancyRecord;

import java.util.UUID;

public interface PregnancyRecordRepository extends JpaRepository<PregnancyRecord, UUID> {
    Page<PregnancyRecord> findByFarmIdOrderByCreatedAtDesc(UUID farmId, Pageable pageable);

    Page<PregnancyRecord> findByCowIdOrderByCreatedAtDesc(UUID cowId, Pageable pageable);
}
