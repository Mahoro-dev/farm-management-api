package rw.farm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rw.farm.domain.Enums.AcquisitionType;
import rw.farm.domain.Enums.AnimalStatus;
import rw.farm.domain.Enums.Sex;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "animals")
public class Animal extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private AnimalSpecies species;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id")
    private CattleBreed breed;

    @Column(name = "animal_number", nullable = false, length = 60)
    private String animalNumber;

    @Column(name = "name", length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false, length = 10)
    private Sex sex;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "birth_date_is_estimated", nullable = false)
    private boolean birthDateEstimated;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "color_markings")
    private String colorMarkings;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquisition_type", nullable = false, length = 20)
    private AcquisitionType acquisitionType = AcquisitionType.BORN_ON_FARM;

    @Column(name = "acquired_on")
    private LocalDate acquiredOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dam_id")
    private Animal dam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sire_id")
    private Animal sire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semen_straw_id")
    private SemenStraw semenStraw;

    @Column(name = "artificial_sire_code", length = 100)
    private String artificialSireCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private AnimalStatus status = AnimalStatus.ACTIVE;

    @Column(name = "status_changed_on")
    private LocalDate statusChangedOn;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "notes")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
