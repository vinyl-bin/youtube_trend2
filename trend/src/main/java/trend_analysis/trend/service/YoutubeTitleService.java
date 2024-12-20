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
                        httpRequest.setConnectTimeout(6 * 60000);  //
                        httpRequest.setReadTimeout(6 * 60000);
                    }
                })
                .setApplicationName("youtube-trendy-shorts")
                .build();




        LocalDateTime startingDate = LocalDateTime.now().toLocalDate().atStartOfDay().minusDays(1);
        LocalDateTime currentDate = startingDate;
        int attempt = 0;
        int viewCountAttempt = 0;
//        int retryCount = 0;
        final int maxAttempts = 8;
        final int maxViewCountAttempts = 2;
//        int maxRetryCount = 2;
        List<Shorts> shortsList = new ArrayList<>();
        String nextPageToken = null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String shortsDate = startingDate.format(formatter);
        System.out.println("\nstartingDate: " + shortsDate);
        shortsList.add(new Shorts(shortsDate, "", -1, -1));

        while (true) {
            LocalDateTime endDate = startingDate.plusDays(1);
            String formattedStartDate = startingDate.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
            String formattedEndDate = endDate.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);

            YouTube.Search.List search = youtube.search().list(Collections.singletonList("id,snippet"));
            search.setKey(apiKey);
            search.setType(Collections.singletonList("video"));
//            search.setOrder("viewCount");
            search.setOrder("rating");
            search.setMaxResults(MAX_RESULTS);
            search.setRegionCode("KR");
            search.setVideoDuration("short");
//            search.setRelevanceLanguage("ko");
//            search.setQ("유행|밈|챌린지|트랜드|트렌드|짤|바이럴|커버|요즘뜨는"); // Adding the keywords   //챌린지, trend, 트랜드, meme
            search.setQ("은|는|이|가|이|기|랑|면|니|우|음|었|았|다|유행|밈|챌린지|트랜드|트렌드|짤|바이럴|커버|요즘");
            search.setPublishedAfter(formattedStartDate);
            search.setPublishedBefore(formattedEndDate);

            System.out.println("Checking videos from: " + formattedStartDate + " to " + formattedEndDate);

            System.out.println("정보 개수: " + shortsList.size() + "  시도 횟수: " + attempt + " viewCount 시도 횟수: " + viewCountAttempt);
            try {
                if (attempt > 0) {
                    Thread.sleep(2000 * attempt); // 이전 오류 시 더 긴 대기 시간을 줍니다. (재시도 대기시간 증가)
                }
                if (endDate.isEqual(currentDate.minusDays(7))) {   // 7일간의 데이터만 가져오기
                    throw new IOException("지난 7일간의 데이터를 모두 가져왔습니다.");
                }

                search.setPageToken(nextPageToken);
                SearchListResponse searchResponse = search.execute();
                List<SearchResult> searchResultList = searchResponse.getItems();

                if (viewCountAttempt >= maxViewCountAttempts) {
                    viewCountAttempt = 0;
//                    retryCount = 0;
                    System.out.println("조회수 시도 횟수가 초과되었습니다.");
                    System.out.println("약 2분간 정지");
                    Thread.sleep(2 * 60000);  //2분? 1분간 정지
                    startingDate = startingDate.minusDays(1);
                    System.out.println("Extended date range by 1 days.");
                    String shortsDateAfter = startingDate.format(formatter);
                    System.out.println("\nstartingDate: " + shortsDateAfter);
                    shortsList.add(new Shorts(shortsDateAfter, "", -1, -1));
                }

                if (searchResultList != null && !searchResultList.isEmpty()) {
                    int viewCount = processSearchResults(searchResultList, youtube, shortsList);
                    System.out.println("viewCount: " + viewCount);
//                    if (viewCount < 1000) {           // 조회수가 1000이상일때만 가져오기
//                        shortsList.remove(shortsList.size() - 1);  // 조회수 1000 미만인 데이터 제거
//                        System.out.println("1000이상의 쇼츠가 아닙니다.");
//                        viewCountAttempt++;
//                    }
                }

                nextPageToken = searchResponse.getNextPageToken();
//                if (nextPageToken == null) {
//                    attempt++;
//                    viewCountAttempt = 0;
//                    System.out.println("더 이상 페이지가 존재하지 않습니다. 하지만 다시 시도해보겠습니다.");
//                    System.out.println("약 2분간 정지");
//                    Thread.sleep(2 * 60000);  //2분? 1분간 정지
//
//                    retryCount++;
//                }

//                if (nextPageToken == null && retryCount > maxRetryCount) {
//                    attempt++;
//                    viewCountAttempt = 0;
//                    retryCount = 0;
//                    System.out.println("더 이상 페이지가 존재하지 않습니다.");
//                    System.out.println("약 2분간 정지");
//                    Thread.sleep(2 * 60000);  //2분? 1분간 정지
//                    startingDate = startingDate.minusDays(1);
//                    System.out.println("Extended date range by 1 days.");
//                    String shortsDateAfter = startingDate.format(formatter);
//                    System.out.println("\nstartingDate: " + shortsDateAfter);
//                    shortsList.add(new Shorts(shortsDateAfter, "", -1, -1));
//                }

                if (nextPageToken == null) {
                    attempt++;
                    viewCountAttempt = 0;
                    System.out.println("더 이상 페이지가 존재하지 않습니다.");
                    System.out.println("약 2분간 정지");
                    Thread.sleep(2 * 60000);  //2분? 1분간 정지
                    startingDate = startingDate.minusDays(1);
                    System.out.println("Extended date range by 1 days.");
                    String shortsDateAfter = startingDate.format(formatter);
                    System.out.println("\nstartingDate: " + shortsDateAfter);
                    shortsList.add(new Shorts(shortsDateAfter, "", -1, -1));
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

        shortsList.remove(shortsList.size() - 1);   //마지막에 들어간 날짜 제거하기
        writeToCSV(shortsList);

    }

    private int processSearchResults(List<SearchResult> searchResultList, YouTube youtube, List<Shorts> shortsList) throws IOException {
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
                BigInteger viewCount = video.getStatistics().getViewCount();
                BigInteger likeCount = video.getStatistics().getLikeCount();

                // null일 경우 0으로 설정
                if (viewCount == null) {
                    viewCount = BigInteger.ZERO;
                }
                if (likeCount == null) {
                    likeCount = BigInteger.ZERO;
                }

                String duration = video.getContentDetails().getDuration();
                long durationInSeconds = parseISO8601Duration(duration);

                if (durationInSeconds <= 120) {
                    int viewCountInt = viewCount.intValue();
                    int likeCountInt = likeCount.intValue();
                    Shorts shorts = new Shorts(videoTitle, videoDescription, viewCountInt, likeCountInt);
                    shortsList.add(shorts);
                    return viewCountInt;
                }
            }
        }
        return 0;
    }

    private long parseISO8601Duration(String duration) {
        Duration dur = Duration.parse(duration);
        return dur.getSeconds();
    }

    private long convertBigIntegerToLong(BigInteger bigInteger) {
        return bigInteger != null ? bigInteger.longValue() : 0L;
    }

    private void writeToCSV(List<Shorts> shortsList) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String currentTime = LocalDateTime.now().format(formatter);
//        String fileName = currentTime + "_youtube_current_viewCount.csv";
        String fileName = currentTime + "_youtube_KR_rating.csv";
//        String fileName = "youtube_title_des_viewCount_meme_2.csv";                   //!--------------수정---------------!
        File csvFile = new File("src/main/resources/csv/" + fileName);

        File directory = new File("src/main/resources/csv");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8);
             CSVWriter csvWriter = new CSVWriter(osw)) {

            for (Shorts shorts : shortsList) {
                String[] shortsData = {
                        shorts.getTitle(),
                        shorts.getDescription(),
                        String.valueOf(shorts.getViewCount()),
                        String.valueOf(shorts.getLikeCount())
                };
                csvWriter.writeNext(shortsData);
            }

            System.out.println("CSV 파일이 UTF-8 형식으로 성공적으로 생성되었습니다.");
        } catch (IOException e) {
            System.out.println("CSV 파일 작성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
