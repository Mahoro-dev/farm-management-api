package rw.farm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.AnimalSpecies;

import java.util.List;
import java.util.Optional;

public interface AnimalSpeciesRepository extends JpaRepository<AnimalSpecies, Short> {
    List<AnimalSpecies> findAllByOrderByNameAsc();

    Optional<AnimalSpecies> findByCodeIgnoreCase(String code);
}
