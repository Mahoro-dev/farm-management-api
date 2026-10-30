package rw.farm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.Role;

public interface RoleRepository extends JpaRepository<Role, String> {
}
