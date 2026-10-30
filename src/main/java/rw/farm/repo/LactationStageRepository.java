package rw.farm.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.farm.domain.LactationStage;

import java.util.List;

public interface LactationStageRepository extends JpaRepository<LactationStage, String> {

    List<LactationStage> findAllByOrderByDisplayOrderAsc();
}
