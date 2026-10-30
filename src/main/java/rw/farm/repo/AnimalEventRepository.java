package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.AnimalEvent;

import java.util.UUID;

public interface AnimalEventRepository extends JpaRepository<AnimalEvent, UUID> {
    Page<AnimalEvent> findByAnimalIdOrderByEventAtDesc(UUID animalId, Pageable pageable);
}
