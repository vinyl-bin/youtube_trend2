package com.cookiek.commenthat.service;

import com.cookiek.commenthat.domain.YoutubeRaw;
import com.cookiek.commenthat.domain.YoutubeTrend;
import com.cookiek.commenthat.repository.YoutubeRawRepository;
import com.cookiek.commenthat.repository.YoutubeTrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.openkoreantext.processor.tokenizer.KoreanTokenizer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import scala.collection.Seq;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeTrendAnalysisService {

    private final YoutubeRawRepository youtubeRawRepository;
    private final YoutubeTrendRepository youtubeTrendRepository;

    private static final String CACHE_FILE_TEMPLATE = "youtube_tokens_%s_%s.txt";
    private static final String STOPWORDS_FILE = "stopwords.txt";  // resources 폴더에 위치

    // 매일 오전 2시에 실행
//    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void analyzeTrends() {
        LocalDate periodEndDate = LocalDate.now();
        LocalDate periodStartDate = periodEndDate.minusWeeks(1);

        String cacheFileName = String.format(CACHE_FILE_TEMPLATE,
                periodStartDate.toString().replaceAll("-", ""),
                periodEndDate.toString().replaceAll("-", "")
        );

        log.info("✅ 트렌드 분석 시작: 기간 {} ~ {}", periodStartDate, periodEndDate);
        log.info("📂 캐시 파일명: {}", cacheFileName);

        List<String> tokens;

        if (Files.exists(Paths.get(cacheFileName))) {
            log.info("📂 캐시 파일이 존재합니다. 파일에서 토큰을 읽습니다: {}", cacheFileName);
            try {
                tokens = Files.readAllLines(Paths.get(cacheFileName))
                        .stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException("캐시 파일 읽기 실패", e);
            }
        } else {
            log.info("🆕 캐시 파일이 없습니다. 토큰화를 진행합니다.");

            List<YoutubeRaw> lastWeekVideos = youtubeRawRepository.findAllByTimeBetween(
                    periodStartDate.atStartOfDay(),
                    periodEndDate.atStartOfDay()
            );
            log.info("📥 수집된 영상 수: {}", lastWeekVideos.size());

            Set<String> excludedPos = Set.of(
                    "Josa", "Eomi", "PreEomi", "Conjunction", "Determiner",
                    "Adverb", "Suffix", "Verb", "Exclamation", "Foreign",
                    "Number", "Punctuation",
                    "Adjective", "KoreanParticle", "Alpha",
                    "ScreenName", "Email", "URL", "CashTag", "Emoji"
            );

            List<String> allTokens = new ArrayList<>();

            for (YoutubeRaw video : lastWeekVideos) {
                String text = video.getTitle() + " " + video.getDes();
                CharSequence normalized = OpenKoreanTextProcessorJava.normalize(text);
                Seq<KoreanTokenizer.KoreanToken> tokenSeq = OpenKoreanTextProcessorJava.tokenize(normalized);
                List<KoreanTokenizer.KoreanToken> tokenList = scala.collection.JavaConverters.seqAsJavaList(tokenSeq);

                Set<String> tokenSet = tokenList.stream()
                        .filter(token -> !excludedPos.contains(token.pos().toString()))
                        .map(token -> {
                            String word = token.text().trim();
                            if (token.pos().toString().equals("Hashtag") && word.startsWith("#") && word.length() > 1) {
                                word = word.substring(1);  // ✅ # 제거
                            }
                            return word;
                        })
                        .filter(word -> word.length() > 1)
                        .filter(word -> word.matches("^[가-힣a-zA-Z]+$"))
                        .collect(Collectors.toSet());  // 🚩 중복 제거

                allTokens.addAll(tokenSet);
            }

            log.info("✅ 토큰화 완료 (불용어 제거 전): {}개", allTokens.size());

            // ✅ 사후 병합 로직
            List<String> mergedTokens = new ArrayList<>();
            for (int i = 0; i < allTokens.size(); i++) {
                String current = allTokens.get(i);
                if (i < allTokens.size() - 1) {
                    String next = allTokens.get(i + 1);
                    if (current.length() >= 2 && next.length() == 1) {
                        String merged = current + next;
                        mergedTokens.add(merged);
                        log.debug("🔗 병합됨: {} + {} → {}", current, next, merged);
                        i++;
                        continue;
                    }
                }
                mergedTokens.add(current);
            }

            log.info("🔗 병합 후 토큰 개수: {}", mergedTokens.size());

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFileName))) {
                for (String word : mergedTokens) {
                    writer.write(word);
                    writer.newLine();
                }
                log.info("📝 병합된 토큰을 캐시 파일로 저장했습니다: {}", cacheFileName);
            } catch (IOException e) {
                throw new RuntimeException("캐시 파일 저장 실패", e);
            }

            tokens = mergedTokens;
        }

        // 불용어 제거
        Set<String> stopWords = loadStopWords();
        List<String> filteredTokens = tokens.stream()
                .filter(word -> !stopWords.contains(word))
                .collect(Collectors.toList());

        log.info("🚫 불용어 제거 후 최종 명사 개수: {}", filteredTokens.size());

        // 단어 카운트
        Map<String, Long> wordCountMap = filteredTokens.stream()
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        log.info("🔢 고유 단어 개수: {}", wordCountMap.size());

        // 상위 1000개 추출
        List<Map.Entry<String, Long>> topWords = wordCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(1000)
                .collect(Collectors.toList());

        log.info("🏆 상위 50개 단어:");
        topWords.forEach(entry ->
                log.info("   {}: {}회", entry.getKey(), entry.getValue())
        );

        // 저장
        for (Map.Entry<String, Long> entry : topWords) {
            YoutubeTrend trend = new YoutubeTrend();
            trend.setWord(entry.getKey());
            trend.setCount(entry.getValue().intValue());
            trend.setPeriodStart(periodStartDate.atStartOfDay());
            trend.setPeriodEnd(periodEndDate.atStartOfDay());
            youtubeTrendRepository.save(trend);
            log.info("💾 저장 완료: {} ({})", entry.getKey(), entry.getValue());
        }

        log.info("🎉 트렌드 분석 완료 ✅");
    }

    /**
     * 불용어 목록을 파일에서 읽어오기
     */
    private Set<String> loadStopWords() {
        try {
            ClassPathResource resource = new ClassPathResource(STOPWORDS_FILE);
            List<String> lines = Files.readAllLines(resource.getFile().toPath());
            Set<String> stopWords = lines.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            log.info("🛑 불용어 {}개 로드 완료", stopWords.size());
            return stopWords;
        } catch (IOException e) {
            throw new RuntimeException("불용어 파일 읽기 실패: " + STOPWORDS_FILE, e);
        }
    }
}
