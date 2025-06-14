package com.cookiek.commenthat.service;

import com.cookiek.commenthat.domain.YoutubeTrend;
import com.cookiek.commenthat.dto.GptRequest;
import com.cookiek.commenthat.dto.GptResponse;
import com.cookiek.commenthat.dto.TrendClassificationResponse;
import com.cookiek.commenthat.repository.YoutubeTrendRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeTrendGptService {

    private final YoutubeTrendRepository youtubeTrendRepository;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private final String GPT_URL = "https://api.openai.com/v1/chat/completions";
    private static final int MAX_KEYWORDS_PER_REQUEST = 50;  // 묶음당 키워드 개수
    private static final int MAX_TOKENS = 5000;              // 최대 토큰 수

    private WebClient webClient;

    @PostConstruct
    private void initWebClient() {
        this.webClient = WebClient.builder()
                .baseUrl(GPT_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .build();
        log.info("✅ WebClient 초기화 완료 (API Key length={})", openAiApiKey.length());
    }

    /**
     * 키워드를 분할해서 GPT로 여러 번 요청 (묶음마다 딜레이 포함)
     */
    public void analyzeKeywordsWithGpt(List<String> keywords) {
        List<List<String>> batches = splitList(keywords, MAX_KEYWORDS_PER_REQUEST);
        log.info("🚀 전체 {}개 키워드를 {}개 묶음으로 분할하여 {}회 요청합니다.",
                keywords.size(), MAX_KEYWORDS_PER_REQUEST, batches.size());

        Flux.fromIterable(batches)
                .concatMap(batch -> {
                    int idx = batches.indexOf(batch) + 1;
                    log.info("📦 [{} / {}] 묶음 처리 대기 중: {}개 키워드", idx, batches.size(), batch.size());
                    return Mono.delay(Duration.ofSeconds(1))
                            .then(processBatchAsyncMono(batch, idx, batches.size()));
                })
                .doOnComplete(() -> log.info("🎉 모든 묶음 요청이 완료되었습니다."))
                // 에러가 발생해도 스트림이 중단되지 않고 계속 실행되도록
                .onErrorContinue((throwable, obj) -> {
                    log.error("💥 스트림 처리 중 예외 발생, 계속 진행합니다: {}", throwable.getMessage());
                })
                // 최종 구독 시에도 에러 콜백을 넘겨줍니다.
                .subscribe(
                        unused -> { /* next onComplete */ },
                        err -> log.error("💥 예상치 못한 최종 에러: {}", err.getMessage())
                );
    }

    /**
     * 단일 묶음을 GPT로 처리 (비동기)
     */
    private Mono<Void> processBatchAsyncMono(List<String> keywords, int batchNumber, int totalBatches) {
        String prompt = buildPrompt(keywords);

        GptRequest request = new GptRequest(
                "gpt-4o",
                List.of(
                        new GptRequest.Message("system", "당신은 유튜브 새로운 사건이나 트렌드, 신조어, 챌린지 키워드를 분류하는 전문가입니다."),
                        new GptRequest.Message("user", prompt)
                ),
                MAX_TOKENS
        );

        return webClient.post()
                .bodyValue(request)
                .retrieve()
                // HTTP 4xx/5xx 를 WebClientResponseException 으로 래핑해 던짐
                .bodyToMono(GptResponse.class)
                // HTTP 오류(401 등)가 나와도 Mono.empty() 로 대체하여 스트림 완전히 중단되지 않도록
                // HTTP 에러 또는 파싱 에러 시 재시도
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                        .filter(e -> e instanceof WebClientResponseException)
                        .doBeforeRetry(rs ->
                                log.warn("🔄 [{} / {}] HTTP 에러, 재시도 {}/2",
                                        batchNumber, totalBatches, rs.totalRetriesInARow() + 1))
                )
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("❌ [{} / {}] 요청 실패 (HTTP {}): {}",
                            batchNumber, totalBatches, e.getRawStatusCode(), e.getMessage());
                    return Mono.empty();
                })
                .flatMap(response -> {
                    if (response == null || response.getChoices().isEmpty()) {
                        log.error("❌ [{} / {}] GPT 응답이 비어있습니다.", batchNumber, totalBatches);
                        return Mono.empty();
                    }

                    String gptContent = response.getChoices().get(0).getMessage().getContent();
                    gptContent = cleanGptResponse(gptContent);

                    log.info("✅ [{} / {}] (정제 후) GPT 응답:\n{}", batchNumber, totalBatches, gptContent);

                    try {
                        List<TrendClassificationResponse> resultList = objectMapper.readValue(
                                gptContent,
                                new TypeReference<List<TrendClassificationResponse>>() {}
                        );
                        saveTrends(resultList, batchNumber, totalBatches);
                    } catch (Exception e) {
                        log.error("❌ [{} / {}] GPT 응답 파싱 실패", batchNumber, totalBatches, e);
                    }

                    return Mono.empty();  // 반드시 리턴해야 chain 유지됨
                });
    }

    /**
     * GPT 응답에서 JSON만 추출
     */
    private String cleanGptResponse(String content) {
        content = content.trim();
        if (content.startsWith("```")) {
            int firstIndex = content.indexOf("\n");
            int lastIndex = content.lastIndexOf("```");
            if (firstIndex != -1 && lastIndex != -1 && lastIndex > firstIndex) {
                return content.substring(firstIndex, lastIndex).trim();
            }
        }
        return content;
    }

    /**
     * GPT 응답 결과를 DB에 업데이트
     */
    private void saveTrends(List<TrendClassificationResponse> results, int batchNumber, int totalBatches) {
        int updatedCount = 0;

        for (TrendClassificationResponse res : results) {
            YoutubeTrend trend = youtubeTrendRepository.findByWord(res.getWord());

            if (trend != null) {
                trend.setStopwordCandidate(res.getStopwordCandidate());
                trend.setNote(res.getNote());
                trend.setCategory(res.getCategory());
                youtubeTrendRepository.save(trend);
                updatedCount++;
                log.info("🔄 [{} / {}] 업데이트 완료: {} (불용어 후보: {}, 비고: {}, 카테고리: {})",
                        batchNumber, totalBatches, res.getWord(), res.getStopwordCandidate(), res.getNote(), res.getCategory());
            } else {
                log.warn("⚠️ [{} / {}] 기존에 존재하지 않아서 업데이트 안됨: {}", batchNumber, totalBatches, res.getWord());
            }
        }

        log.info("✅ [{} / {}] 총 {}개 키워드 업데이트 완료", batchNumber, totalBatches, updatedCount);
    }

    /**
     * GPT 프롬프트 생성
     */
    private String buildPrompt(List<String> keywords) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음은 유튜브 트렌드 키워드 목록입니다. 각 키워드에 대해 다음을 작성해 주세요.\n\n")
                .append("✅ 반드시 지켜야 할 기준:\n")
                .append("1️⃣ 불용어 여부: 의미가 약하거나 일반적인 단어(예: '여기', '눌러주세요', '확인', '투어', '설교', '타워', '금지', '계좌', '멤버십' 등)는 모두 true로 해 주세요.\n")
                .append("   ✔️ 특히 **트렌드, 사건, 신조어, 유행 등과 직접적으로 관련 없는 단어**는 무조건 불용어(true)로 처리합니다.\n")
                .append("   ✔️ 예: '투어', '도전', '알고리즘', '금지', '도시', '타워', '계좌', '라이벌', '설교' 등은 특별한 의미가 없으므로 불용어로 분류.\n\n")
                .append("2️⃣ 비고: 간단한 이유를 적어 주세요. (왜 불용어인지 또는 왜 의미 있는지)\n\n")
                .append("3️⃣ 카테고리: 불용어가 false인 경우만 다음 중 하나로 분류해 주세요.\n")
                .append("- 시사: 정치/뉴스/사회 이슈 (예: '윤석열', '탄핵', '속보')\n")
                .append("- 밈: 유행어/밈/인터넷 트렌드 (예: '말랑이', '잼못타', '로블록스')\n")
                .append("- 챌린지: 챌린지 콘텐츠 (예: '댄스 챌린지', '댄스')\n")
                .append("- 연예/방송: 연예/콘텐츠 (예: '백종원', '드라마', '아이돌')\n")
                .append("- 이외: 일반 관심사 (예: '게임', '여행', '강아지')\n\n")
                .append("🚨 **불용어가 true이면 category는 반드시 null로 해 주세요.**\n\n")
                .append("결과는 아래 JSON 배열 형식으로만 출력해 주세요.\n\n")
                .append("[\n")
                .append("  {\n")
                .append("    \"word\": \"김문수\",\n")
                .append("    \"stopwordCandidate\": false,\n")
                .append("    \"note\": \"정치인 이름으로 최근 이슈와 연관\",\n")
                .append("    \"category\": \"시사\"\n")
                .append("  },\n")
                .append("  {\n")
                .append("    \"word\": \"투어\",\n")
                .append("    \"stopwordCandidate\": true,\n")
                .append("    \"note\": \"단순 여행 관련 일반 단어로 트렌드 아님\",\n")
                .append("    \"category\": null\n")
                .append("  }\n")
                .append("]\n\n")
                .append("키워드 목록:\n");


        for (String word : keywords) {
            sb.append("- ").append(word).append("\n");
        }
        return sb.toString();
    }

    /**
     * 리스트를 n개씩 쪼개는 함수
     */
    private <T> List<List<T>> splitList(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }
}
