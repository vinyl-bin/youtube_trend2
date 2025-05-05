package trend_analysis.trend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import trend_analysis.trend.service.YoutubeTrendingService;

@Component
public class YoutubeFetchScheduler {

    private final YoutubeTrendingService service;

    public YoutubeFetchScheduler(YoutubeTrendingService service) {
        this.service = service;
    }

    // 매 30분마다 실행
    @Scheduled(cron = "0 15,45 * * * *", zone = "Asia/Seoul")
    public void fetchTask() {
        try {
            service.fetchAndSaveLastQuarter();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}