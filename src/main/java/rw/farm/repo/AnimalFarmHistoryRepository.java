package rw.farm.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.AnimalFarmHistory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnimalFarmHistoryRepository extends JpaRepository<AnimalFarmHistory, UUID> {
    @EntityGraph(attributePaths = "farm")
    Optional<AnimalFarmHistory> findByAnimalIdAndLeftOnIsNull(UUID animalId);

    List<AnimalFarmHistory> findByAnimalIdOrderByJoinedOnDesc(UUID animalId);

    @EntityGraph(attributePaths = "farm")
    List<AnimalFarmHistory> findByAnimalIdInAndLeftOnIsNull(Collection<UUID> animalIds);

    List<AnimalFarmHistory> findByFarmIdAndLeftOnIsNullOrderByJoinedOnDesc(UUID farmId);
}
