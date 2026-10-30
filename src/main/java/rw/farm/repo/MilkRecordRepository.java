package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.MilkRecord;

import java.time.LocalDate;
import java.util.UUID;

public interface MilkRecordRepository extends JpaRepository<MilkRecord, UUID> {
    @EntityGraph(attributePaths = {"animal", "shift"})
    Page<MilkRecord> findByFarmIdAndMilkingDateBetweenOrderByMilkingDateDescCreatedAtDesc(
            UUID farmId, LocalDate from, LocalDate to, Pageable pageable);

    Page<MilkRecord> findByAnimalIdOrderByMilkingDateDesc(UUID animalId, Pageable pageable);
}
