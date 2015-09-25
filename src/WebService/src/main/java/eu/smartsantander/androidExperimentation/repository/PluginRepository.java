package eu.smartsantander.androidExperimentation.repository;

import eu.smartsantander.androidExperimentation.model.Plugin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.Set;

/**
 * @author Dimitrios Amaxilatis.
 */
public interface PluginRepository extends CrudRepository<Plugin, Long> {

    Set<Plugin> findAll();
    Set<Plugin> findByContextTypeIsIn(Set<String> contextType);

    Page<Plugin> findAll(Pageable pageable);

}