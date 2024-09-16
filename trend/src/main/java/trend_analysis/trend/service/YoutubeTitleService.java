package trend_analysis.trend.service;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import trend_analysis.trend.dto.Shorts;
import com.opencsv.CSVWriter;

import java.io.*;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Service
public class YoutubeTitleService {
    @Value("${youtube.api.key}")
    private String apiKey;

    private static final long MAX_RESULTS = 50L;  // 최대 결과 수

    public void getPopularShortsToCSV() throws IOException {
        JsonFactory jsonFactory = new JacksonFactory();
        HttpTransport httpTransport = new NetHttpTransport();

        YouTube youtube = new YouTube.Builder(
                httpTransport,
                jsonFactory,
                new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest httpRequest) {
                        // Set timeouts
                        httpRequest.setConnectTimeout(6 * 60000);  // 3 minutes
                        httpRequest.setReadTimeout(6 * 60000);     // 3 minutes
                    }
                })
                .setApplicationName("youtube-popular-shorts")
                .build();

        YouTube.Search.List search = youtube.search().list(Collections.singletonList("id,snippet"));
        search.setKey(apiKey);
        search.setType(Collections.singletonList("video"));
//        search.setOrder("viewCount");
//        search.setOrder("rating");
        search.setOrder("date");                  //!--------------수정---------------!
        search.setMaxResults(MAX_RESULTS);
        search.setRegionCode("KR");
        search.setVideoDuration("short");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fourteenDaysAgo = now.minusDays(14);
        String formattedDate = fourteenDaysAgo.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
        search.setPublishedAfter(formattedDate);

        List<Shorts> shortsList = new ArrayList<>();
        String nextPageToken = null;

        while (shortsList.size() < 1000) {
            try {
                search.setPageToken(nextPageToken);
                SearchListResponse searchResponse = search.execute();
                List<SearchResult> searchResultList = searchResponse.getItems();

                if (searchResultList != null && !searchResultList.isEmpty()) {
                    processSearchResults(searchResultList, youtube, shortsList);
                }

                nextPageToken = searchResponse.getNextPageToken();
                if (nextPageToken == null) {
                    break; // 더 이상 페이지가 없으면 종료
                }
            } catch (IOException e) {
                System.out.println("API 요청 중 오류 발생: " + e.getMessage());
                // 여기서까지 도달한 데이터 저장
                writeToCSV(shortsList);
                break;
            }
        }

        writeToCSV(shortsList);
    }

    private void processSearchResults(List<SearchResult> searchResultList, YouTube youtube, List<Shorts> shortsList) throws IOException {
        for (SearchResult searchResult : searchResultList) {
            String videoId = searchResult.getId().getVideoId();
            String videoTitle = searchResult.getSnippet().getTitle();
            String videoDescription = searchResult.getSnippet().getDescription();
            String videoUrl = "https://www.youtube.com/shorts/" + videoId;

            YouTube.Videos.List videos = youtube.videos().list(Collections.singletonList("statistics,contentDetails"));
            videos.setKey(apiKey);
            videos.setId(Collections.singletonList(videoId));
            VideoListResponse videoResponse = videos.execute();
            List<Video> videoList = videoResponse.getItems();

            if (!videoList.isEmpty()) {
                Video video = videoList.get(0);
                long views = convertBigIntegerToLong(video.getStatistics().getViewCount());
                String duration = video.getContentDetails().getDuration();
                long durationInSeconds = parseISO8601Duration(duration);

                if (durationInSeconds <= 60) {
                    Shorts shorts = new Shorts(videoTitle, videoDescription, videoUrl, views);
                    shortsList.add(shorts);
                }
            }
        }
    }

    private long parseISO8601Duration(String duration) {
        Duration dur = Duration.parse(duration);
        return dur.getSeconds();
    }

    private long convertBigIntegerToLong(BigInteger bigInteger) {
        return bigInteger != null ? bigInteger.longValue() : 0L;
    }

    private void writeToCSV(List<Shorts> shortsList) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String currentTime = LocalDateTime.now().format(formatter);
        String fileName = currentTime + "_youtube_title_des_date.csv";                   //!--------------수정---------------!
        File csvFile = new File("src/main/resources/csv/" + fileName);

        File directory = new File("src/main/resources/csv");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8);
             CSVWriter csvWriter = new CSVWriter(osw)) {

            String[] header = { "Title", "Description" };
            csvWriter.writeNext(header);

            for (Shorts shorts : shortsList) {
                String[] shortsData = {
                        shorts.getTitle(),
                        shorts.getDescription()
                };
                csvWriter.writeNext(shortsData);
            }

            System.out.println("CSV 파일이 UTF-8 형식으로 성공적으로 생성되었습니다.");
        } catch (IOException e) {
            System.out.println("CSV 파일 작성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
