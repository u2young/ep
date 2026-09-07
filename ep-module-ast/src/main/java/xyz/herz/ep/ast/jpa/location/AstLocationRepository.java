package xyz.herz.ep.ast.jpa.location;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.ast.entity.location.AstLocation;

import java.util.List;
import java.util.Optional;

public interface AstLocationRepository extends JpaRepository<AstLocation, Long> {

    Optional<AstLocation> findByCode(String code);

    List<AstLocation> findByStatus(Integer status);
}
