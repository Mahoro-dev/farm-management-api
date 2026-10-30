package rw.farm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 128)
    private String tableName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "record_id", length = 36)
    private UUID recordId;

    @Column(name = "action", nullable = false, length = 10)
    private String action;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "changed_by", length = 36)
    private UUID changedBy;

    @Column(name = "transaction_id")
    private Long transactionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_data")
    private String oldData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data")
    private String newData;
}
