package anam_145.SpringBoot.Server.repository;

import anam_145.SpringBoot.Server.domain.aiGuide.MiniAppCodeIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MiniAppCodeIndexRepository extends JpaRepository<MiniAppCodeIndex, String> {

    Optional<MiniAppCodeIndex> findByAppId(String appId);

    boolean existsByAppId(String appId);

    void deleteByAppId(String appId);
}
