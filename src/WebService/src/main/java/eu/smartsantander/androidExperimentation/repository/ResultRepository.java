package eu.smartsantander.androidExperimentation.repository;

import eu.smartsantander.androidExperimentation.model.Result;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;

import java.util.Set;

/**
 * @author Dimitrios Amaxilatis.
 */
public interface ResultRepository extends CrudRepository<Result, Long> {

    Page<Result> findAll(Pageable pageable);

    Set<Result> findByExperimentId(long experimentId);
}