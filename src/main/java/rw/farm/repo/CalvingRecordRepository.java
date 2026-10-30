package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.CalvingRecord;

import java.util.UUID;

public interface CalvingRecordRepository extends JpaRepository<CalvingRecord, UUID> {
    Page<CalvingRecord> findByFarmIdOrderByCalvedAtDesc(UUID farmId, Pageable pageable);

    Page<CalvingRecord> findByDamIdOrderByCalvedAtDesc(UUID damId, Pageable pageable);
}
