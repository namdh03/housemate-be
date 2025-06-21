package housemate.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import housemate.constants.ServiceConfiguration;
import housemate.entities.ServiceConfig;

@Repository
public interface ServiceConfigRepository extends JpaRepository<ServiceConfig, Integer> {

	@Query(value = "SELECT * FROM service_config sf "
			+ "WHERE LOWER(sf.config_type) COLLATE utf8mb4_bin = LOWER(:configType) "
			+ "AND LOWER(sf.config_value) COLLATE utf8mb4_bin = LOWER(:configValue)", nativeQuery = true)
	Optional<ServiceConfig> findByConfigTypeAndConfigValue(@Param("configType") String configType,
			@Param("configValue") String configValue);

	ServiceConfig findFirstByConfigType(ServiceConfiguration configType);

	List<ServiceConfig> findAllByConfigType(ServiceConfiguration configType);

}
