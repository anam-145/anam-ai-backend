package anam_145.SpringBoot.Server.repository;

import anam_145.SpringBoot.Server.domain.aiGuide.ScreenInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ScreenInfo 엔티티에 대한 데이터 접근 Repository
 *
 * MiniApp의 화면 정보를 조회하고 관리한다.
 */
@Repository
public interface ScreenInfoRepository extends JpaRepository<ScreenInfo, Long> {

    /**
     * 특정 MiniApp의 모든 화면을 조회한다.
     *
     * @param appId MiniApp ID
     * @return 해당 앱의 모든 화면 목록
     */
    List<ScreenInfo> findByAppId(String appId);

    /**
     * MiniApp ID와 화면 이름으로 화면을 조회한다.
     *
     * @param appId MiniApp ID
     * @param name 화면 이름 (예: "TransferScreen")
     * @return 화면 정보 (존재하지 않으면 empty)
     */
    Optional<ScreenInfo> findByAppIdAndName(String appId, String name);

    /**
     * 특정 MiniApp의 모든 화면을 이름 순으로 정렬하여 조회한다.
     *
     * @param appId MiniApp ID
     * @return 이름 순으로 정렬된 화면 목록
     */
    @Query("SELECT s FROM ScreenInfo s WHERE s.appId = :appId ORDER BY s.name")
    List<ScreenInfo> findAllByAppIdOrderByName(@Param("appId") String appId);

    /**
     * 특정 MiniApp의 모든 화면을 삭제한다.
     * cascade 설정에 의해 관련된 ComposableInfo도 함께 삭제된다.
     *
     * @param appId MiniApp ID
     */
    void deleteByAppId(String appId);
}
