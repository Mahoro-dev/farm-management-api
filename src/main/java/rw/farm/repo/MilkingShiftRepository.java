package rw.farm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.MilkingShift;

import java.util.List;
import java.util.UUID;

public interface MilkingShiftRepository extends JpaRepository<MilkingShift, UUID> {
    List<MilkingShift> findByFarmIdOrderByDisplayOrderAscNameAsc(UUID farmId);
}
