package anam_145.SpringBoot.Server.repository;

import anam_145.SpringBoot.Server.domain.aiGuide.ScreenInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenInfoRepository extends JpaRepository<ScreenInfo, Long> {

    List<ScreenInfo> findByAppId(String appId);

    Optional<ScreenInfo> findByAppIdAndName(String appId, String name);

    @Query("SELECT s FROM ScreenInfo s WHERE s.appId = :appId ORDER BY s.name")
    List<ScreenInfo> findAllByAppIdOrderByName(@Param("appId") String appId);

    void deleteByAppId(String appId);
}
