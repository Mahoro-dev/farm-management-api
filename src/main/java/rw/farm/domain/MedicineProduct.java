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
import rw.farm.domain.Enums.MedicineForm;
import rw.farm.domain.Enums.MedicineUnit;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "medicine_products")
public class MedicineProduct extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "active_ingredient", length = 160)
    private String activeIngredient;

    @Enumerated(EnumType.STRING)
    @Column(name = "form", nullable = false, length = 30)
    private MedicineForm form;

    @Column(name = "formulation", length = 100)
    private String formulation;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_unit", nullable = false, length = 30)
    private MedicineUnit baseUnit;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "description")
    private String description;

    @Column(name = "requires_prescription", nullable = false)
    private boolean requiresPrescription;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
