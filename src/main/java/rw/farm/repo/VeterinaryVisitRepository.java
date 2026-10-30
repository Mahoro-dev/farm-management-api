package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.Enums.VisitType;
import rw.farm.domain.VeterinaryVisit;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VeterinaryVisitRepository extends JpaRepository<VeterinaryVisit, UUID> {
    Page<VeterinaryVisit> findByFarmIdOrderByVisitAtDesc(UUID farmId, Pageable pageable);

    Page<VeterinaryVisit> findByAnimalIdOrderByVisitAtDesc(UUID animalId, Pageable pageable);

    Optional<VeterinaryVisit> findFirstByFarmIdAndAnimalIdAndVisitAtAndVisitType(
            UUID farmId, UUID animalId, Instant visitAt, VisitType visitType);
}
