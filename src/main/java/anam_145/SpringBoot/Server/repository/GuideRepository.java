package anam_145.SpringBoot.Server.repository;

import anam_145.SpringBoot.Server.domain.aiGuide.GuideEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * GuideEntity 엔티티에 대한 데이터 접근 Repository
 *
 * AI가 생성한 가이드 정보를 조회하고 관리한다.
 * 가이드 재사용이나 통계 분석에 활용할 수 있다.
 */
@Repository
public interface GuideRepository extends JpaRepository<GuideEntity, String> {

    /**
     * 가이드 ID로 가이드를 조회한다.
     *
     * @param guideId 가이드 ID
     * @return 가이드 정보 (존재하지 않으면 empty)
     */
    Optional<GuideEntity> findByGuideId(String guideId);

    /**
     * 특정 MiniApp의 모든 가이드를 조회한다.
     *
     * @param appId MiniApp ID
     * @return 해당 앱의 모든 가이드 목록
     */
    List<GuideEntity> findByAppId(String appId);

    /**
     * 특정 기간 이후에 생성된 가이드를 최신순으로 조회한다.
     * 최근 사용자 질문 패턴 분석에 활용할 수 있다.
     *
     * @param appId MiniApp ID
     * @param fromDate 시작 날짜
     * @return 최신순으로 정렬된 가이드 목록
     */
    @Query("SELECT g FROM GuideEntity g WHERE g.appId = :appId " +
           "AND g.createdAt >= :fromDate ORDER BY g.createdAt DESC")
    List<GuideEntity> findRecentGuides(@Param("appId") String appId,
                                         @Param("fromDate") LocalDateTime fromDate);

    /**
     * 특정 의도(intent)에 해당하는 가이드를 조회한다.
     * 유사한 질문에 대해 이전 가이드를 재사용할 수 있다.
     *
     * @param appId MiniApp ID
     * @param intent 사용자 의도 (예: "TRANSFER", "RECEIVE")
     * @return 해당 의도의 가이드 목록
     */
    List<GuideEntity> findByAppIdAndIntent(String appId, String intent);

    /**
     * 특정 MiniApp의 모든 가이드를 삭제한다.
     * cascade 설정에 의해 관련된 GuideStepEntity도 함께 삭제된다.
     *
     * @param appId MiniApp ID
     */
    void deleteByAppId(String appId);
}
