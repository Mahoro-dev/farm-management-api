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
import rw.farm.domain.Enums.AdministrationRoute;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "treatment_medications")
public class TreatmentMedication extends UuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "treatment_id", nullable = false)
    private Treatment treatment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_product_id", nullable = false)
    private MedicineProduct medicineProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private MedicineBatch batch;

    @Column(name = "dose_amount", nullable = false, precision = 14, scale = 3)
    private BigDecimal doseAmount;

    @Column(name = "dose_unit", nullable = false, length = 30)
    private String doseUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "route", nullable = false, length = 30)
    private AdministrationRoute route;

    @Column(name = "administrations_count", nullable = false)
    private short administrationsCount = 1;

    @Column(name = "quantity_used", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityUsed;

    @Column(name = "frequency", length = 100)
    private String frequency;

    @Column(name = "duration_text", length = 100)
    private String durationText;

    @Column(name = "milk_withdrawal_hours")
    private Integer milkWithdrawalHours;

    @Column(name = "meat_withdrawal_hours")
    private Integer meatWithdrawalHours;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
