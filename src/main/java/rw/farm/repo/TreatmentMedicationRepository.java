package rw.farm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.TreatmentMedication;

import java.util.List;
import java.util.UUID;

public interface TreatmentMedicationRepository extends JpaRepository<TreatmentMedication, UUID> {
    List<TreatmentMedication> findByTreatmentIdOrderByCreatedAtAsc(UUID treatmentId);
}
