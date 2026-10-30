package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.Treatment;

import java.util.UUID;

public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {
    Page<Treatment> findByFarmIdOrderByTreatmentDateDesc(UUID farmId, Pageable pageable);

    Page<Treatment> findByAnimalIdOrderByTreatmentDateDesc(UUID animalId, Pageable pageable);
}
