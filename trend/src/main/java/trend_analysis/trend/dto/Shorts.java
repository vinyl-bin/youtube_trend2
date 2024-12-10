package trend_analysis.trend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class Shorts {

    private String title;
    private String description;
    private int viewCount;
    private int likeCount;

    public Shorts(String title, String description, int viewCount, int likeCount) {
        this.title = title;
        this.description = description;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
    }
}
