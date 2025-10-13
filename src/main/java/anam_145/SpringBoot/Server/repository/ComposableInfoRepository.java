package anam_145.SpringBoot.Server.repository;

import anam_145.SpringBoot.Server.domain.aiGuide.ComposableInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ComposableInfo 엔티티에 대한 데이터 접근 Repository
 *
 * UI 요소 정보를 조회, 검색, 삭제하는 기능을 제공한다.
 * 특히 키워드 기반 검색 기능이 핵심이며, Simple RAG 아키텍처의 검색 레이어를 담당한다.
 */
@Repository
public interface ComposableInfoRepository extends JpaRepository<ComposableInfo, Long> {

    /**
     * 특정 MiniApp의 모든 UI 요소를 조회한다.
     *
     * @param appId MiniApp ID
     * @return 해당 앱의 모든 UI 요소 목록
     */
    List<ComposableInfo> findByAppId(String appId);

    /**
     * 특정 MiniApp에서 특정 타입의 UI 요소만 조회한다.
     *
     * @param appId MiniApp ID
     * @param type UI 요소 타입 (예: "Button", "TextField")
     * @return 해당 타입의 UI 요소 목록
     */
    List<ComposableInfo> findByAppIdAndType(String appId, String type);

    /**
     * 특정 MiniApp에서 composableId로 UI 요소를 조회한다.
     *
     * @param appId MiniApp ID
     * @param composableId UI 요소 식별자 (예: "btn_send")
     * @return 매칭되는 UI 요소 목록 (같은 ID를 가진 요소가 여러 개일 수 있음)
     */
    @Query("SELECT c FROM ComposableInfo c WHERE c.appId = :appId AND c.composableId = :composableId")
    List<ComposableInfo> findByAppIdAndComposableId(@Param("appId") String appId,
                                                      @Param("composableId") String composableId);

    /**
     * 키워드 기반 UI 요소 검색 (Simple RAG의 핵심 기능)
     *
     * 사용자 질문에서 추출한 키워드를 사용하여 관련 UI 요소를 찾는다.
     * LIKE 검색을 사용하며, 다음 필드들을 대상으로 검색한다:
     * - searchableText: 통합 검색 텍스트
     * - text: 표시 텍스트
     * - semanticHint: 의미적 힌트
     * - onClickCode: 클릭 동작 코드
     *
     * 검색 결과는 관련도에 따라 정렬된다:
     * 1순위: semanticHint 매칭 (가장 정확한 의미)
     * 2순위: text 매칭 (사용자에게 보이는 텍스트)
     * 3순위: onClickCode 매칭 (동작 코드)
     * 4순위: 기타
     *
     * @param appId MiniApp ID
     * @param keyword 검색 키워드 (예: "송금", "transfer")
     * @return 관련도 순으로 정렬된 UI 요소 목록
     */
    @Query("SELECT c FROM ComposableInfo c WHERE c.appId = :appId " +
           "AND (c.searchableText LIKE %:keyword% " +
           "OR c.text LIKE %:keyword% " +
           "OR c.semanticHint LIKE %:keyword% " +
           "OR c.onClickCode LIKE %:keyword%) " +
           "ORDER BY " +
           "CASE WHEN c.semanticHint LIKE %:keyword% THEN 1 " +
           "WHEN c.text LIKE %:keyword% THEN 2 " +
           "WHEN c.onClickCode LIKE %:keyword% THEN 3 " +
           "ELSE 4 END")
    List<ComposableInfo> searchByKeyword(@Param("appId") String appId,
                                           @Param("keyword") String keyword);

    /**
     * MySQL Full-Text Search를 사용한 고급 검색 (향후 구현)
     *
     * MySQL FULLTEXT 인덱스를 활용하여 더 빠르고 정확한 검색을 수행한다.
     * MATCH...AGAINST 구문을 사용하며, 자연어 검색을 지원한다.
     *
     * 사용하려면 다음 SQL을 먼저 실행해야 한다:
     * <pre>
     * ALTER TABLE composable_info
     * ADD FULLTEXT INDEX ft_search (searchable_text, text, semantic_hint, onclick_code);
     * </pre>
     *
     * 현재는 주석 처리되어 있으며, 필요시 활성화할 수 있다.
     */
    /*
    @Query(value = "SELECT * FROM composable_info " +
                   "WHERE app_id = :appId " +
                   "AND MATCH(searchable_text, text, semantic_hint, onclick_code) " +
                   "AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
                   "ORDER BY MATCH(searchable_text, text, semantic_hint, onclick_code) " +
                   "AGAINST(:keyword IN NATURAL LANGUAGE MODE) DESC " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<ComposableInfo> fullTextSearch(@Param("appId") String appId,
                                         @Param("keyword") String keyword,
                                         @Param("limit") int limit);
    */

    /**
     * 특정 화면에 속한 모든 UI 요소를 조회한다.
     *
     * @param appId MiniApp ID
     * @param screenName 화면 이름 (예: "TransferScreen")
     * @return 해당 화면의 모든 UI 요소 목록
     */
    @Query("SELECT c FROM ComposableInfo c " +
           "WHERE c.screenInfo.name = :screenName AND c.appId = :appId")
    List<ComposableInfo> findByScreenName(@Param("appId") String appId,
                                            @Param("screenName") String screenName);

    /**
     * 특정 MiniApp의 모든 UI 요소를 삭제한다.
     * 주로 재인덱싱 시 기존 데이터를 정리하는데 사용된다.
     *
     * @param appId MiniApp ID
     */
    void deleteByAppId(String appId);

    /**
     * 특정 MiniApp의 UI 요소 개수를 조회한다.
     * 인덱싱 완료 후 통계 정보로 사용된다.
     *
     * @param appId MiniApp ID
     * @return UI 요소 개수
     */
    long countByAppId(String appId);
}
