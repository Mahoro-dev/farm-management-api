package rw.farm.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.farm.domain.AuditLog;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:tableName is null or a.tableName = :tableName)
              and (:recordId is null or a.recordId = :recordId)
            """)
    Page<AuditLog> search(
            @Param("tableName") String tableName,
            @Param("recordId") UUID recordId,
            Pageable pageable);
}
