package trend_analysis.trend.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TrendRepository {

    private final EntityManager em;

    // 이후 코드 추가

}
