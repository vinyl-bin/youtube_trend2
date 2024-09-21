package trend_analysis.trend.service;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

@Service
public class KeywordService implements IWordAnalysisService {

    Komoran nlp = null;

    private static final String[] FILE_PATHS = {
            "src/main/resources/csv/20240919_222926_youtube_current_viewCount_KRkeyword.csv"
    };

    public KeywordService() {
        this.nlp = new Komoran(DEFAULT_MODEL.LIGHT); //학습데이터 버전
    }

    @Override
    public List<String> doWordNouns(String text) throws Exception {
        String replace_text = text.replaceAll("[^가-힣a-zA-Z0-9]", " ");   //한국어, 영어, 숫자 제외 단어 모두 빈칸으로 변환
//        String replace_text = text.replaceAll("[^가-힣0-9]", " ");
        String trim_text = replace_text.trim();  // 분석할 문장 앞, 뒤에 존재할 수 있는 필요 없는 공백 제거

        KomoranResult analyzeResultList = this.nlp.analyze(trim_text);
//        List<String> rList = analyzeResultList.getNouns();  //명사만 가져오기
        List<String> rList = analyzeResultList.getMorphesByTags(
                "NNG", "NNP", // 명사
                "VV", "VA",   // 동사, 형용사
                "MM",         // 관형사
                "MAG",        // 일반 부사
                "IC",         // 감탄사
                "XR",         // 어근
                "XPN",        // 접두사
                "XSA",        // 형용사 파생 접미사
                "XSN"         // 명사 파생 접미사
        );

        // 중복을 제거하기 위해 Set 사용
        Set<String> wordSet = new HashSet<>(rList);
        rList = new ArrayList<>(wordSet);

        if (rList == null) {
            rList = new ArrayList<String>();
        }

        return rList;
    }

    @Override
    public Map<String, Integer> doWordCount(List<String> pList) throws Exception {
        if (pList ==null) {
            pList = new ArrayList<String>();
        }

        Map<String, Integer> rMap = new HashMap<>();

        //맵의 키는 중복되면 안되기 때문에 set을 이용하여 중복 제거
        Set<String> rSet = new HashSet<String>(pList);
        Iterator<String> it = rSet.iterator();

        while(it.hasNext()) {
            String word = it.next();

            // 단어가 null이면 빈 문자열로 처리
            if (word == null) {
                word = "";
            }

            //단어가 중복 저장되어 있는 pList로부터 단어의 빈도수 가져오기
            int frequency = Collections.frequency(pList, word);

            rMap.put(word, frequency);
        }
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
                if(rList == null) {
                    rList = new ArrayList<String>();
                }
                Map<String, Integer> rMap = this.doWordCount(rList);

                if(rMap == null) {
                    rMap = new HashMap<String, Integer>();
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
