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
import rw.farm.domain.Enums.TreatmentOutcome;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "treatments")
public class Treatment extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinary_visit_id")
    private VeterinaryVisit veterinaryVisit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Column(name = "treatment_date", nullable = false)
    private Instant treatmentDate;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "diagnosis")
    private String diagnosis;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "treatment_plan")
    private String treatmentPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 30)
    private TreatmentOutcome outcome = TreatmentOutcome.IN_PROGRESS;

    @Column(name = "milk_withdrawal_until")
    private Instant milkWithdrawalUntil;

    @Column(name = "meat_withdrawal_until")
    private Instant meatWithdrawalUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
