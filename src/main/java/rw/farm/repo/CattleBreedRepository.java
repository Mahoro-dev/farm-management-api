package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.CattleBreed;

import java.util.UUID;

public interface CattleBreedRepository extends JpaRepository<CattleBreed, UUID> {
    Page<CattleBreed> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
