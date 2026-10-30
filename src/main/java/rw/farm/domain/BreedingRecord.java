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
import rw.farm.domain.Enums.BreedingMethod;
import rw.farm.domain.Enums.BreedingOutcome;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "breeding_records")
public class BreedingRecord extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cow_id", nullable = false)
    private Animal cow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sire_id")
    private Animal sire;

    @Column(name = "breeding_at", nullable = false)
    private Instant breedingAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "breeding_method", nullable = false, length = 30)
    private BreedingMethod breedingMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 30)
    private BreedingOutcome outcome = BreedingOutcome.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User technician;

    @Column(name = "technician_name", length = 160)
    private String technicianName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semen_straw_id")
    private SemenStraw semenStraw;

    @Column(name = "semen_batch_number", length = 100)
    private String semenBatchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ancestry_reviewed_by")
    private User ancestryReviewedBy;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "ancestry_override_reason")
    private String ancestryOverrideReason;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "notes")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
