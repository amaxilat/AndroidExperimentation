package eu.smartsantander.androidExperimentation.repository;

import eu.smartsantander.androidExperimentation.model.Smartphone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;


/**
 * @author Dimitrios Amaxilatis.
 */
public interface SmartphoneRepository extends CrudRepository<Smartphone, Long> {

    Page<Smartphone> findAll(Pageable pageable);

    List<Smartphone> findByPhoneId(int phoneId);
}