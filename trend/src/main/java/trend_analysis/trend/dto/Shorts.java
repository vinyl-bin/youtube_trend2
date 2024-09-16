package trend_analysis.trend.dto;

import java.util.List;

public class Shorts {

    private String title;
    private String description;
    private String url;
    private long views;

    public Shorts(String title, String description, String url, long views) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.views = views;
    }

    public String getTitle() {return title;}
    public String getDescription() {return description;}
    public String getUrl() {return url;}
    public long getViews() {return views;}

    public void setTitle(String title) {this.title = title;}
    public void setDescription(String description) {this.description = description;}
    public void setUrl(String url) {this.url = url;}
    public void setViews(long views) {this.views = views;}
}
