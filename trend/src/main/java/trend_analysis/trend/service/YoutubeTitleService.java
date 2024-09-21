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
                .setApplicationName("youtube-trendy-shorts")
                .build();




        LocalDateTime startingDate = LocalDateTime.now().minusDays(1);
        int attempt = 0;
        final int maxAttempts = 6;
        List<Shorts> shortsList = new ArrayList<>();
        String nextPageToken = null;

//        shortsList.size() < 15
        while (true) {
            LocalDateTime endDate = startingDate.plusDays(1);
            String formattedStartDate = startingDate.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
            String formattedEndDate = endDate.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);

            YouTube.Search.List search = youtube.search().list(Collections.singletonList("id,snippet"));
            search.setKey(apiKey);
            search.setType(Collections.singletonList("video"));
            search.setOrder("viewCount");
//            search.setOrder("rating");
            search.setMaxResults(MAX_RESULTS);
            search.setRegionCode("KR");
            search.setVideoDuration("short");
            search.setQ("유행|밈|챌린지|트랜드|트렌드|짤|바이럴|커버|요즘뜨는"); // Adding the keywords   //챌린지, trend, 트랜드, meme
            search.setPublishedAfter(formattedStartDate);
            search.setPublishedBefore(formattedEndDate);

            System.out.println("Checking videos from: " + formattedStartDate + " to " + formattedEndDate);

            System.out.println("정보 개수: " + shortsList.size() + "  시도 횟수: " + attempt);
            try {
                if (attempt > 0) {
                    Thread.sleep(2000 * attempt); // 이전 오류 시 더 긴 대기 시간을 줍니다. (재시도 대기시간 증가)
                }

                search.setPageToken(nextPageToken);
                SearchListResponse searchResponse = search.execute();
                List<SearchResult> searchResultList = searchResponse.getItems();

                if (searchResultList != null && !searchResultList.isEmpty()) {
                    processSearchResults(searchResultList, youtube, shortsList);
                }

                nextPageToken = searchResponse.getNextPageToken();
                if (nextPageToken == null) {
                    attempt++;
                    System.out.println("더 이상 페이지가 존재하지 않습니다.");
                    System.out.println("약 2분간 정지");
                    Thread.sleep(2 * 60000);  //2분? 1분간 정지
                    startingDate = startingDate.minusDays(1);
                    System.out.println("Extended date range by 1 days.");
                }

                if (attempt >= maxAttempts) {
                    break;
                }


            } catch (IOException e) {
                attempt++;
                System.out.println("API 요청 중 오류 발생: " + e.getMessage());

                if (attempt == maxAttempts) {
                    // 최대 시도 횟수가 초과되었을 때 수집된 데이터를 저장하고 루프 종료
                    writeToCSV(shortsList);
                    System.out.println("최대 시도 횟수를 초과하였습니다.");
                    break;
                } else {
                    System.out.println("재시도 " + attempt + " 중...");
                }
            } catch (InterruptedException e) {
                System.out.println("쓰레드가 인터럽트되었습니다: " + e.getMessage());
                Thread.currentThread().interrupt(); // 인터럽트 상태 복원
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

            YouTube.Videos.List videos = youtube.videos().list(Collections.singletonList("statistics,contentDetails"));
            videos.setKey(apiKey);
            videos.setId(Collections.singletonList(videoId));
            VideoListResponse videoResponse = videos.execute();
            List<Video> videoList = videoResponse.getItems();

            if (!videoList.isEmpty()) {
                Video video = videoList.get(0);
                String duration = video.getContentDetails().getDuration();
                long durationInSeconds = parseISO8601Duration(duration);

                if (durationInSeconds <= 60) {
                    Shorts shorts = new Shorts(videoTitle, videoDescription);
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
        String fileName = currentTime + "_youtube_current_viewCount_KRkeyword.csv";
//        String fileName = "youtube_title_des_viewCount_meme_2.csv";                   //!--------------수정---------------!
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
