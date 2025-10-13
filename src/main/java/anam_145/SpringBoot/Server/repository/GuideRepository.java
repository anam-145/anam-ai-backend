package anam_145.SpringBoot.Server.repository;

import anam_145.SpringBoot.Server.domain.aiGuide.GuideEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GuideRepository extends JpaRepository<GuideEntity, String> {

    Optional<GuideEntity> findByGuideId(String guideId);

    List<GuideEntity> findByAppId(String appId);

    @Query("SELECT g FROM GuideEntity g WHERE g.appId = :appId " +
           "AND g.createdAt >= :fromDate ORDER BY g.createdAt DESC")
    List<GuideEntity> findRecentGuides(@Param("appId") String appId,
                                         @Param("fromDate") LocalDateTime fromDate);

    List<GuideEntity> findByAppIdAndIntent(String appId, String intent);

    void deleteByAppId(String appId);
}
