package trend_analysis.trend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import trend_analysis.trend.dto.Shorts;
//import trend_analysis.trend.service.YouTubeService;
import trend_analysis.trend.service.Keyword2Service;
import trend_analysis.trend.service.KeywordService;
import trend_analysis.trend.service.YoutubeTitleService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ShortsController {

//    private final YouTubeService youtubeService;
    private final YoutubeTitleService youtubeTitleService;
    private final KeywordService keywordService;
    private final Keyword2Service keyword2Service;

    // GET 요청을 통해 인기 있는 쇼츠의 오디오 목록을 반환
//    @GetMapping("/chart")
//    public String getPopularShorts(Model model) {
//        try {
//            List<Shorts> shortsList = youtubeService.getPopularShorts();
//            model.addAttribute("shorts", shortsList);
//            return "chart"; // chart.html
//        } catch (IOException e) {
//            model.addAttribute("error", "Error fetching popular shorts: " + e.getMessage());
//            return "error"; // error.html
//        }
//    }

    //get youtube title, des
    @GetMapping("/getCSV")
    public ResponseEntity<String> getCSV() {
        try {
            // CSV 파일 생성 및 저장
            youtubeTitleService.getPopularShortsToCSV();

            // 성공적인 응답 반환
            return ResponseEntity.ok("CSV 파일이 성공적으로 생성되었습니다.");
        } catch (IOException e) {
            e.printStackTrace();
            // 실패 응답 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("CSV 파일 생성 중 오류 발생");
        }
    }

    @GetMapping("/keyword")
    public String analyzeKeywords(Model model) {
        try {
            // KeywordService의 doWordAnalysis 메서드를 호출
            Map<String, Integer> keywordMap = keyword2Service.doWordAnalysis();

            // 모델에 데이터 추가
            model.addAttribute("keywordMap", keywordMap);

            return "keyword"; // 이 템플릿 이름이 src/main/resources/templates/keyword.html과 일치해야 합니다
        } catch (Exception e) {
            // 예외 처리
            e.printStackTrace();
            return "error"; // 에러 페이지 템플릿으로 리다이렉트하거나 빈 데이터를 표시
        }
    }

//    @GetMapping("/keyword")
//    public String analyzeKeywords(Model model) {
//        try {
//            // KeywordService의 doWordAnalysis 메서드를 호출
//            Map<String, Integer> keywordMap = keywordService.doWordAnalysis();
//
//            // 모델에 데이터 추가
//            model.addAttribute("keywordMap", keywordMap);
//
//            return "keyword"; // 이 템플릿 이름이 src/main/resources/templates/keyword.html과 일치해야 합니다
//        } catch (Exception e) {
//            // 예외 처리
//            e.printStackTrace();
//            return "error"; // 에러 페이지 템플릿으로 리다이렉트하거나 빈 데이터를 표시
//        }
//    }

}