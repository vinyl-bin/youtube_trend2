package trend_analysis.trend.service;

import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.openkoreantext.processor.tokenizer.KoreanTokenizer;
import org.openkoreantext.processor.KoreanTokenJava;
import org.springframework.stereotype.Service;
import scala.collection.Seq;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class Keyword2Service implements IWordAnalysisService {

//    private static Set<String> removeSet = new HashSet<>();

    LocalDateTime startingDate = LocalDateTime.now().toLocalDate().atStartOfDay().minusDays(1);

    private static final String[] FILE_PATHS = {
//            "src/main/resources/csv/20240919_222926_youtube_current_viewCount_KRkeyword.csv"
//            "src/main/resources/csv/20240925_youtube_KR_rating.csv",
//            "src/main/resources/csv/20240925_youtube_KR_viewCount.csv",
            "src/main/resources/csv/20241210_youtube_KR_rating.csv",
            "src/main/resources/csv/20241220_youtube_KR_rating.csv"
    };

    public Keyword2Service() {
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



    }



    @Override
    public Map<String, Integer> doWordCount(List<String> pList) throws Exception {
        if (pList == null) {
            pList = new ArrayList<>();
        }

        Map<String, Integer> rMap = new HashMap<>();

//        // 맵의 키는 중복되면 안되기 때문에 set을 이용하여 중복 제거
//        Set<String> rSet = new HashSet<>(pList);
//        Iterator<String> it = rSet.iterator();
//
//        while (it.hasNext()) {
//            String word = it.next();
//
//            // 단어가 null이면 빈 문자열로 처리
//            if (word == null) {
//                word = "";
//            }
//
//            // 단어가 중복 저장되어 있는 pList로부터 단어의 빈도수 가져오기
//            int frequency = Collections.frequency(pList, word);
//
//            rMap.put(word, frequency);
//        }
//        return rMap;


        for (String word : pList) {
            int frequency = 0;

            if (word == null || word.matches("\\d+") || word.matches("[a-zA-Z]+") || word.matches("유행|밈|챌린지|트랜드|트렌드|짤|바이럴|커버|요즘")) {
                word = "";
            }

            for (String word2 : pList) {
                if (word.equals("")){
                    continue;
                } else if (word.equals(word2)) {
                    frequency++;
                } else if (word2.contains(word)) {
                    frequency++;
//                    removeSet.add(word2);
                }

            }
            rMap.put(word, frequency);
        }

//        // removeList에 있는 요소들을 rMap에서 제거
//        for (String wordToRemove : removeList) {
//            rMap.remove(wordToRemove);  // removeList의 단어를 rMap에서 제거
//        }

        return rMap;

    }

    @Override
    public Map<String, Integer> doWordAnalysis() throws Exception {
        Map<String, Integer> totalWordCountMap = new HashMap<>();

        // 3개의 CSV 파일을 순차적으로 처리
        for (String csvFile : FILE_PATHS) {
            BufferedReader reader = new BufferedReader(new FileReader(csvFile));
            String line;

            // 파일의 각 줄을 읽어서 처리
            while ((line = reader.readLine()) != null) {
                List<String> rList = this.doWordNouns(line);
                if (rList == null) {
                    rList = new ArrayList<>();
                }
                Map<String, Integer> rMap = this.doWordCount(rList);

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
            reader.close(); // 파일을 다 읽었으면 닫기
        }


        return sortByValueDescending(totalWordCountMap);
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

