package rw.farm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rw.farm.domain.Enums.CalvingOutcome;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "calving_records")
public class CalvingRecord extends UuidEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregnancy_record_id")
    private PregnancyRecord pregnancyRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dam_id", nullable = false)
    private Animal dam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sire_id")
    private Animal sire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semen_straw_id")
    private SemenStraw semenStraw;

    @Column(name = "artificial_sire_code", length = 100)
    private String artificialSireCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calf_id")
    private Animal calf;

    @Column(name = "calved_at", nullable = false)
    private Instant calvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 30)
    private CalvingOutcome outcome;

    @Column(name = "assistance_required", nullable = false)
    private boolean assistanceRequired;

    @Column(name = "number_of_calves", nullable = false)
    private short numberOfCalves = 1;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "complications")
    private String complications;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
