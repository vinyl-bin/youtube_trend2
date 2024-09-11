package trend_analysis.trend.service;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import trend_analysis.trend.dto.Shorts;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class YouTubeService {

    @Value("${youtube.api.key}")
    private String apiKey;

    public List<Shorts> getPopularShorts() throws IOException {
        // JSON 데이터를 처리하기 위한 JsonFactory 객체 생성
        JsonFactory jsonFactory = new JacksonFactory();

        // YouTube 객체를 빌드하여 API에 접근할 수 있는 YouTube 클라이언트 생성
        YouTube youtube = new YouTube.Builder(
                new com.google.api.client.http.javanet.NetHttpTransport(),
                jsonFactory,
                request -> {})
                .setApplicationName("youtube-popular-shorts")
                .build();

        // YouTube Search API를 사용하여 인기 쇼츠 동영상 검색
        YouTube.Search.List search = youtube.search().list(Collections.singletonList("id,snippet"));

        // API 키 설정
        search.setKey(apiKey);

        // 현재 시간 구하기
        LocalDateTime now = LocalDateTime.now();
        // 현재 시간에서 1달을 빼기
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        // ISO 8601 형식으로 변환
        String formattedDate = oneMonthAgo.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("현재 시간에서 1달 전의 ISO 8601 형식 시간대: " + formattedDate);

        search.setType(Collections.singletonList("video"));
        search.setOrder("viewCount"); // 조회수 기준으로 정렬
        search.setMaxResults(100L); // 최대 100개의 동영상 가져오기
        search.setRegionCode("KR");
        search.setVideoDuration("short");   //4분 미만 영상
        search.setPublishedAfter(formattedDate);  //현재 시간 기준 한달 전 영상부터 표시

        // 요청 실행
        SearchListResponse searchResponse = search.execute();
        List<SearchResult> searchResultList = searchResponse.getItems();

        // 결과를 저장할 리스트
        List<Shorts> shortsList = new ArrayList<>();

        if (searchResultList != null && !searchResultList.isEmpty()) {
            for (SearchResult searchResult : searchResultList) {
                String videoId = searchResult.getId().getVideoId();
                String videoTitle = searchResult.getSnippet().getTitle();
                String videoUrl = "https://www.youtube.com/shorts/" + videoId; // 쇼츠 URL

                // 조회수 정보 가져오기
                YouTube.Videos.List videos = youtube.videos().list(Collections.singletonList("statistics"));
                videos.setKey(apiKey);
                videos.setId(Collections.singletonList(videoId));
                VideoListResponse videoResponse = videos.execute();
                List<Video> videoList = videoResponse.getItems();
                long views = videoList.isEmpty() ? 0 : convertBigIntegerToLong(videoList.get(0).getStatistics().getViewCount());

                // Shorts DTO 객체 생성 및 추가
                Shorts shorts = new Shorts(videoTitle, videoUrl, views);
                shortsList.add(shorts);
            }
            return shortsList;
        }
        return Collections.emptyList();
    }

    private long convertBigIntegerToLong(BigInteger bigInteger) {
        return bigInteger != null ? bigInteger.longValue() : 0L;
    }
}
