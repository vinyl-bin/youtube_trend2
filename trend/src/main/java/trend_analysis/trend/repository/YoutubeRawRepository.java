package trend_analysis.trend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trend_analysis.trend.domain.YoutubeRaw;

@Repository
public interface YoutubeRawRepository extends JpaRepository<YoutubeRaw, Long> {}

