package com.cookiek.commenthat.controller;

import com.cookiek.commenthat.dto.CategoryTrendDto;
import com.cookiek.commenthat.dto.TrendDto;
import com.cookiek.commenthat.repository.YoutubeTrendRepository;
import com.cookiek.commenthat.service.TrendPerCategoryService;
import com.cookiek.commenthat.service.YoutubeTrendAnalysisService;
import com.cookiek.commenthat.service.YoutubeTrendGptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class YoutubeTrendController {

    private final YoutubeTrendAnalysisService trendService;
    private final YoutubeTrendGptService youtubeTrendGptService;
    private final YoutubeTrendRepository youtubeTrendRepository;
    private final TrendPerCategoryService trendPerCategoryService;


    private static final Map<String, String> englishToKorean = Map.of(
            "current", "시사",
            "meme", "밈",
            "challenge", "챌린지",
            "entertainment", "연예/방송",
            "others", "이외"
    );

    @GetMapping("/analyze-trends")
    @ResponseBody
    public void analyzeTrends() {
        trendService.analyzeTrends();
        System.out.println("트렌드 분석이 완료되었습니다.");
    }

    @GetMapping("/gpt-trends")
    @ResponseBody
    public void gptTrends() {
        List<String> keywords = youtubeTrendRepository.findWordsWithNullNote();

        youtubeTrendGptService.analyzeKeywordsWithGpt(keywords);
        System.out.println("gpt 분석이 완료되었습니다.");
    }

    @GetMapping("/trends/view")
    public String viewTrends(Model model) {
        List<CategoryTrendDto> trendsByCategory = trendPerCategoryService.getTopTrendsPerCategory();
        model.addAttribute("categories", trendsByCategory);
        return "trendView";  // templates/trendView.html로 연결
    }

    @GetMapping("/trend/{category}")
    public String showCategoryTrend(@PathVariable String category, Model model) throws Exception {
//        LocalDateTime periodEnd = LocalDateTime.of(2025, 6, 2, 0, 0, 0, 0);
        String koreanCategory = englishToKorean.getOrDefault(category, "이외");
//        List<TrendDto> trends = youtubeTrendRepository.findByCategory(koreanCategory, periodEnd);
        List<TrendDto> trends = youtubeTrendRepository.findByCategory(koreanCategory);

        // trends를 JSON 문자열로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String trendsJson = objectMapper.writeValueAsString(trends);

        model.addAttribute("category", koreanCategory);
        model.addAttribute("trendsJson", trendsJson);
        return "category-trend";
    }
}
