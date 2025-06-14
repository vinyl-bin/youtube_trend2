package com.cookiek.commenthat.repository;

import com.cookiek.commenthat.domain.YoutubeTrend;
import com.cookiek.commenthat.dto.TrendDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface YoutubeTrendRepository extends JpaRepository<YoutubeTrend, Long> {

    @Query(value = """
    SELECT * FROM youtube_trend
    WHERE word = :word
    ORDER BY 
        CASE WHEN note IS NULL THEN 0 ELSE 1 END,
        periodEnd DESC
    LIMIT 1
""", nativeQuery = true)
    YoutubeTrend findByWord(@Param("word") String word);

    @Query("SELECT yt.word FROM YoutubeTrend yt WHERE yt.note IS NULL")
    List<String> findWordsWithNullNote();


    /**
     * 최신 데이터(periodEnd) 중에서
     *
     * 카테고리별 상위 20개 키워드만 추출
     *
     * 그리고 전체 결과를 카테고리 → count 순으로 정렬
     */
    @Query(value = """
    SELECT t.category, t.word, t.note, t.count
    FROM (
        SELECT
            *,
            ROW_NUMBER() OVER (PARTITION BY category ORDER BY count DESC) as rn
        FROM youtube_trend
        WHERE stopwordCandidate = false
          AND periodEnd = (SELECT MAX(periodEnd) FROM youtube_trend)
    ) as t
    WHERE t.rn <= 20
    ORDER BY t.category, t.count DESC;
""", nativeQuery = true)
    List<Object[]> findTop20PerCategory();


    @Query("""
    SELECT new com.cookiek.commenthat.dto.TrendDto(yt.word, yt.count)
    FROM YoutubeTrend yt
    WHERE yt.category = :category
      AND yt.stopwordCandidate = false
      AND yt.periodEnd = (SELECT MAX(y.periodEnd) FROM YoutubeTrend y)
    ORDER BY yt.count DESC
""")
    List<TrendDto> findByCategory(
            @Param("category") String category);

}
