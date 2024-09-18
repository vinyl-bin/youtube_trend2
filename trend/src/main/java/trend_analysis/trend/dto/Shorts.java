package trend_analysis.trend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class Shorts {

    private String title;
    private String description;

    public Shorts(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
