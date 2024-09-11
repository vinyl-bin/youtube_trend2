package trend_analysis.trend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import trend_analysis.trend.dto.Shorts;
import trend_analysis.trend.service.YouTubeService;

import java.io.IOException;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ShortsController {

    private final YouTubeService youtubeService;

    // GET 요청을 통해 인기 있는 쇼츠의 오디오 목록을 반환
    @GetMapping("/chart")
    public String getPopularShorts(Model model) {
        try {
            List<Shorts> shortsList = youtubeService.getPopularShorts();
            model.addAttribute("shorts", shortsList);
            return "chart"; // chart.html
        } catch (IOException e) {
            model.addAttribute("error", "Error fetching popular shorts: " + e.getMessage());
            return "error"; // error.html
        }
    }
}