package rw.farm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lactation_stages")
public class LactationStage {

    @Id
    @Column(name = "code", length = 40, nullable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "meaning", nullable = false, length = 200)
    private String meaning;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;
}
