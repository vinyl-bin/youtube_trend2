package trend_analysis.trend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Trend {

    @Id @GeneratedValue
    @Column(name = "trend_id")
    private Long id;

    private String video_name;
    private String video_url;
    private Double video_score;

}
