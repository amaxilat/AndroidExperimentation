package eu.smartsantander.androidExperimentation.repository;

import eu.smartsantander.androidExperimentation.model.Result;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.Set;

/**
 * @author Dimitrios Amaxilatis.
 */
public interface ResultRepository extends CrudRepository<Result, Long> {

    Page<Result> findAll(Pageable pageable);

    Set<Result> findByExperimentId(int experimentId);

    Set<Result> findByExperimentIdAndDeviceId(int experimentId, int deviceId);

    Set<Result> findByExperimentIdAndTimestampAfter(int experimentId, long start);

    Set<Result> findByExperimentIdAndDeviceIdAndTimestampAfter(int experimentId, int deviceId, long start);

    Set<Result> findByDeviceIdAndTimestampBetween(int deviceId, long start, long end);

    Set<Result> findByDeviceIdAndTimestampAfter(int deviceId, long start);

    Set<Result> findByDeviceIdAndTimestampIsBetween(int deviceId, long start, long end);

}