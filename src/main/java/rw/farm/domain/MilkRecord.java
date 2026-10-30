package rw.farm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "milk_records")
public class MilkRecord extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private MilkingShift shift;

    @Column(name = "milking_date", nullable = false)
    private LocalDate milkingDate;

    @Column(name = "quantity_liters", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantityLiters;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lactation_stage")
    private LactationStage lactationStage;

    @Column(name = "fat_percent", precision = 5, scale = 2)
    private BigDecimal fatPercent;

    @Column(name = "protein_percent", precision = 5, scale = 2)
    private BigDecimal proteinPercent;

    @Column(name = "somatic_cell_count")
    private Integer somaticCellCount;

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
