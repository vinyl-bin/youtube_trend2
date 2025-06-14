package com.cookiek.commenthat.service;

import com.cookiek.commenthat.dto.CategoryTrendDto;
import com.cookiek.commenthat.dto.TrendItemDto;
import com.cookiek.commenthat.repository.YoutubeTrendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class TrendPerCategoryService {

    private final YoutubeTrendRepository youtubeTrendRepository;

    public List<CategoryTrendDto> getTopTrendsPerCategory() {

        List<Object[]> results = youtubeTrendRepository.findTop20PerCategory();

        Map<String, List<TrendItemDto>> grouped = new LinkedHashMap<>();

        for (Object[] row : results) {
            String category = (String) row[0];
            String word = (String) row[1];
            String note = (String) row[2];
            Integer count = (Integer) row[3];

            grouped.computeIfAbsent(category, k -> new ArrayList<>())
                    .add(new TrendItemDto(word, note, count));
        }

        // 한글 → 영어 변환 맵 (이건 클래스 상단에 static final로 빼놓는게 깔끔)
        Map<String, String> categoryToEnglish = Map.of(
                "시사", "current",
                "밈", "meme",
                "챌린지", "challenge",
                "연예/방송", "entertainment",
                "이외", "others"
        );

        return grouped.entrySet().stream()
                .map(e -> new CategoryTrendDto(
                        categoryToEnglish.getOrDefault(e.getKey(), "others"),  // 영어로 변환
                        e.getValue()
                ))
                .toList();
    }

}
