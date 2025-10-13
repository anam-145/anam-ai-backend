package anam_145.SpringBoot.Server.repository;

import anam_145.SpringBoot.Server.domain.aiGuide.ComposableInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComposableInfoRepository extends JpaRepository<ComposableInfo, Long> {

    List<ComposableInfo> findByAppId(String appId);

    List<ComposableInfo> findByAppIdAndType(String appId, String type);

    @Query("SELECT c FROM ComposableInfo c WHERE c.appId = :appId AND c.composableId = :composableId")
    List<ComposableInfo> findByAppIdAndComposableId(@Param("appId") String appId,
                                                      @Param("composableId") String composableId);

    /**
     * Full-Text Search using MySQL LIKE (simple version)
     * For MySQL Full-Text Index, you'll need native query
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
     * MySQL Full-Text Search (requires FULLTEXT index)
     * Uncomment this when you create FULLTEXT index in MySQL
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

    @Query("SELECT c FROM ComposableInfo c " +
           "WHERE c.screenInfo.name = :screenName AND c.appId = :appId")
    List<ComposableInfo> findByScreenName(@Param("appId") String appId,
                                            @Param("screenName") String screenName);

    void deleteByAppId(String appId);

    long countByAppId(String appId);
}
