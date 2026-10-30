package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.farm.domain.Vaccination;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VaccinationRepository extends JpaRepository<Vaccination, UUID> {
    Page<Vaccination> findByFarmIdOrderByAdministeredAtDesc(UUID farmId, Pageable pageable);

    Page<Vaccination> findByAnimalIdOrderByAdministeredAtDesc(UUID animalId, Pageable pageable);

    @Query("""
            select v from Vaccination v
            where v.farm.id = :farmId and v.nextDueOn is not null and v.nextDueOn <= :until
            order by v.nextDueOn
            """)
    List<Vaccination> dueBy(@Param("farmId") UUID farmId, @Param("until") LocalDate until);
}
