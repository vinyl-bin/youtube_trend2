package trend_analysis.trend.service;

import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.openkoreantext.processor.tokenizer.KoreanTokenizer;
import org.openkoreantext.processor.KoreanTokenJava;
import org.springframework.stereotype.Service;
import scala.collection.Seq;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Keyword3Service implements IWordAnalysis2Service {

//    private static Set<String> removeSet = new HashSet<>();


    LocalDateTime startingDate = LocalDateTime.now().toLocalDate().atStartOfDay().minusDays(1);

    private static final String[] FILE_PATHS = {
//            "src/main/resources/csv/20240919_222926_youtube_current_viewCount_KRkeyword.csv"
            "src/main/resources/csv/20240925_youtube_KR_rating.csv",
            "src/main/resources/csv/20240925_youtube_KR_viewCount.csv"
    };

    public Keyword3Service() {
        // 생성자에서 특별한 초기화 작업이 필요하지 않음
    }

    @Override
    public List<String> doWordNouns(String text) throws Exception {
        // 텍스트에서 한국어, 영어, 숫자 제외한 나머지 문자 공백으로 대체
        String replace_text = text.replaceAll("[^가-힣a-zA-Z0-9]", " ");
        String trim_text = replace_text.trim();  // 공백 제거

        // 띄어쓰기로 분리
        String[] words = trim_text.split("\\s+");
        List<String> rList = new ArrayList<>();

        // 각 단어를 검사
        for (int i = 0; i < words.length; i++) {
            String word = words[i];

            //문장에서 명사 추출
            Seq<KoreanTokenizer.KoreanToken> tokens = OpenKoreanTextProcessorJava.tokenize(word);
            List<KoreanTokenJava> tokenList = OpenKoreanTextProcessorJava.tokensToJavaKoreanTokenList(tokens);

            StringBuilder sb = new StringBuilder();  // 조사 이전의 토큰을 합칠 StringBuilder

            for (int j = 0; j < tokenList.size(); j++) {
                KoreanTokenJava token = tokenList.get(j);

                if (token.getPos().toString().equals("Josa")) {
                    // 조사인 경우 앞의 모든 토큰을 합쳐서 rList에 추가
                    if (sb.length() > 0) { // 이전에 합쳐진 토큰이 있을 때만 추가
                        rList.add(sb.toString().trim());
                        sb.setLength(0);  // StringBuilder 초기화
                    }
                } else {
                    // 조사 이외의 토큰은 StringBuilder에 계속 추가
                    sb.append(token.getText());
                }
            }

            // 마지막 토큰들도 처리 (문장의 끝에 조사가 없을 때를 대비)
            if (sb.length() > 0) {
                rList.add(sb.toString().trim());
            }


        }


        // 중복 제거
        Set<String> wordSet = new HashSet<>(rList);
        rList = new ArrayList<>(wordSet);

        return rList;

//        // 띄어쓰기로 분리
//        String[] words = trim_text.split("\\s+");
//        List<String> rList = new ArrayList<>();
//
//        // 각 단어를 검사하고 리스트에 추가
//        Collections.addAll(rList, words);
//
//        // 중복 제거
//        Set<String> wordSet = new HashSet<>(rList);
//        rList = new ArrayList<>(wordSet);
//
//        return rList;

    }



    @Override
    public Map<String, Integer> doWordCount(List<String> pList, String dateStr, int viewCount, int likeCount) throws Exception {
        if (pList == null) {
            pList = new ArrayList<>();
        }

//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 올바른 패턴 설정
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


        // 문자열을 LocalDateTime으로 변환
        LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
        System.out.println("Parsed date and time: " + dateTime);

        // startingDate 설정 (임의의 값)
//            LocalDateTime startingDate = LocalDateTime.now();  // 현재 시간을 startingDate로 설정

        long daysBetween = ChronoUnit.DAYS.between(dateTime, startingDate);


        // 날짜에 따른 가중치 계산 (날짜가 가까울수록 가중치가 높음)
        double dateWeight = 1.0 - (daysBetween * 0.1);
        if (dateWeight < 0.1) dateWeight = 0.1; // 최소 가중치

        // 조회수 대비 좋아요 수 비율 계산 (likeCount / viewCount)
        double likeRatio = (viewCount > 0) ? (double) likeCount / viewCount : 0;
        double likeWeight = 1.0 + (likeRatio * 2); // 좋아요 비율에 따른 가중치 (최소 1, 최대 3)



        Map<String, Integer> rMap = new HashMap<>();

        for (String word : pList) {
            int frequency = 0;

            if (word == null) {
                word = "";
            }

            for (String word2 : pList) {
                if (word.equals(word2)) {
                    frequency++;
                } else if (word2.contains(word)) {
                    frequency++;
//                    removeSet.add(word2);
                }

            }
            // 가중치를 빈도수에 적용
            int weightedFrequency = (int) (frequency * dateWeight * likeWeight);
            rMap.put(word, weightedFrequency);
        }


        return rMap;

    }

    @Override
    public Map<String, Integer> doWordAnalysis() throws Exception {
        Map<String, Integer> totalWordCountMap = new HashMap<>();
        String regex = "\"([^\"]*)\"|([^,]+)";
        Pattern pattern = Pattern.compile(regex);

        // 3개의 CSV 파일을 순차적으로 처리
        for (String csvFile : FILE_PATHS) {
            try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                String line;
                String currentDate = null;
                int viewCount = 0;
                int likeCount = 0;

                // 파일의 각 줄을 읽어서 처리
                while ((line = reader.readLine()) != null) {
                    List<String> fields = new ArrayList<>();
                    Matcher matcher = pattern.matcher(line);

                    // 정규식을 사용해 필드를 추출
                    while (matcher.find()) {
                        if (matcher.group(1) != null) {
                            // 그룹 1: 따옴표로 묶인 필드
                            fields.add(matcher.group(1).trim());
                        } else if (matcher.group(2) != null) {
                            // 그룹 2: 따옴표로 묶이지 않은 필드
                            fields.add(matcher.group(2).trim());
                        }
                    }

                    // 필드 수가 부족한 경우 다음 라인으로 넘어감
                    if (fields.size() < 4) continue;

                    // 날짜 필드 체크 (-1일 경우 날짜로 인식)
                    if (fields.get(2).equals("-1") && fields.get(3).equals("-1")) {
                        currentDate = fields.get(0); // 날짜 저장
                    } else {
                        // 조회수와 좋아요 수
                        viewCount = Integer.parseInt(fields.get(2).trim());
                        likeCount = Integer.parseInt(fields.get(3).trim());

                        // 키라인 생성
                        String keyLine = fields.get(0) + fields.get(1);

                        // 명사 추출 및 단어 빈도수 계산
                        List<String> rList = this.doWordNouns(keyLine);
                        if (rList == null) {
                            rList = new ArrayList<>();
                        }
                        Map<String, Integer> rMap = this.doWordCount(rList, currentDate, viewCount, likeCount);

                        if (rMap == null) {
                            rMap = new HashMap<>();
                        }

                        // 단어 빈도수를 합산
                        for (Map.Entry<String, Integer> entry : rMap.entrySet()) {
                            String word = entry.getKey();
                            int count = entry.getValue();

                            // 기존에 존재하는 단어면 기존 값에 더하기
                            totalWordCountMap.put(word, totalWordCountMap.getOrDefault(word, 0) + count);
                        }
                    }
                }
            }
        }
        return totalWordCountMap;
    }

    private Map<String, Integer> sortByValueDescending(Map<String, Integer> map) {
        // 엔트리를 값 기준으로 내림차순 정렬
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        // 정렬된 결과를 새로운 LinkedHashMap에 저장
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

}

