package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.farm.domain.Animal;
import rw.farm.domain.Enums.AnimalStatus;
import rw.farm.domain.Enums.Sex;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnimalRepository extends JpaRepository<Animal, UUID> {

    @Query(value = """
            select a from Animal a
            where a.organization.id = :orgId
              and (:status is null or a.status = :status)
              and (:sex is null or a.sex = :sex)
              and (:q is null or lower(a.animalNumber) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.name, '')) like lower(concat('%', :q, '%')))
              and (:farmId is null or exists (
                  select h.id from AnimalFarmHistory h
                  where h.animal = a and h.farm.id = :farmId and h.leftOn is null))
              and (:restrict = false or exists (
                  select h2.id from AnimalFarmHistory h2
                  where h2.animal = a and h2.leftOn is null and h2.farm.id in :farmIds))
            order by a.animalNumber
            """,
            countQuery = """
            select count(a) from Animal a
            where a.organization.id = :orgId
              and (:status is null or a.status = :status)
              and (:sex is null or a.sex = :sex)
              and (:q is null or lower(a.animalNumber) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.name, '')) like lower(concat('%', :q, '%')))
              and (:farmId is null or exists (
                  select h.id from AnimalFarmHistory h
                  where h.animal = a and h.farm.id = :farmId and h.leftOn is null))
              and (:restrict = false or exists (
                  select h2.id from AnimalFarmHistory h2
                  where h2.animal = a and h2.leftOn is null and h2.farm.id in :farmIds))
            """)
    Page<Animal> search(
            @Param("orgId") UUID orgId,
            @Param("status") AnimalStatus status,
            @Param("sex") Sex sex,
            @Param("q") String q,
            @Param("farmId") UUID farmId,
            @Param("restrict") boolean restrict,
            @Param("farmIds") Collection<UUID> farmIds,
            Pageable pageable);

    @Query("select a.id, a.dam.id, a.sire.id from Animal a where a.organization.id = :orgId")
    List<Object[]> parentLinks(@Param("orgId") UUID orgId);

    @Query("""
            select a from Animal a
            where exists (
                select h.id from AnimalFarmHistory h
                where h.animal = a and h.farm.id = :farmId and h.leftOn is null)
            order by a.animalNumber
            """)
    List<Animal> findPresent(@Param("farmId") UUID farmId);

    Optional<Animal> findByOrganizationIdAndAnimalNumberIgnoreCase(UUID organizationId, String animalNumber);
}
