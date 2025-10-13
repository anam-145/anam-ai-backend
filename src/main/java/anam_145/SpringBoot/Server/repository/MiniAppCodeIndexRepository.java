package anam_145.SpringBoot.Server.repository;

import anam_145.SpringBoot.Server.domain.aiGuide.MiniAppCodeIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MiniAppCodeIndex 엔티티에 대한 데이터 접근 Repository
 *
 * MiniApp의 코드 인덱스 정보를 관리한다.
 * 주로 MiniApp 승인 및 재인덱싱 시 사용된다.
 */
@Repository
public interface MiniAppCodeIndexRepository extends JpaRepository<MiniAppCodeIndex, String> {

    /**
     * MiniApp ID로 코드 인덱스를 조회한다.
     *
     * @param appId MiniApp ID
     * @return 코드 인덱스 (존재하지 않으면 empty)
     */
    Optional<MiniAppCodeIndex> findByAppId(String appId);

    /**
     * 해당 MiniApp의 코드 인덱스가 존재하는지 확인한다.
     *
     * @param appId MiniApp ID
     * @return 존재 여부
     */
    boolean existsByAppId(String appId);

    /**
     * 특정 MiniApp의 코드 인덱스를 삭제한다.
     * cascade 설정에 의해 관련된 ScreenInfo와 ComposableInfo도 함께 삭제된다.
     *
     * @param appId MiniApp ID
     */
    void deleteByAppId(String appId);
}
