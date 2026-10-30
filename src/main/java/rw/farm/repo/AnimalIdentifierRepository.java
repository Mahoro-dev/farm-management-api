package rw.farm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.farm.domain.AnimalIdentifier;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AnimalIdentifierRepository extends JpaRepository<AnimalIdentifier, UUID> {
    List<AnimalIdentifier> findByAnimalIdOrderByCreatedAtDesc(UUID animalId);

    List<AnimalIdentifier> findByAnimalIdInAndPrimaryTrueAndRetiredAtIsNull(Collection<UUID> animalIds);

    List<AnimalIdentifier> findByAnimalIdInAndRetiredAtIsNull(Collection<UUID> animalIds);

    @Query("""
            select i from AnimalIdentifier i
            where i.animal.organization.id = :orgId
              and i.retiredAt is null
              and lower(i.identifierValue) = lower(:value)
            """)
    List<AnimalIdentifier> findActiveByValue(@Param("orgId") UUID orgId, @Param("value") String value);

    @Modifying(flushAutomatically = true)
    @Query("""
            update AnimalIdentifier i
            set i.primary = false
            where i.animal.id = :animalId and i.primary = true and i.retiredAt is null
            """)
    int clearPrimary(@Param("animalId") UUID animalId);
}
