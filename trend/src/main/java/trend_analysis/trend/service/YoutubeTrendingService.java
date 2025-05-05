package trend_analysis.trend.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import trend_analysis.trend.domain.YoutubeRaw;
import trend_analysis.trend.repository.YoutubeRawRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class YoutubeTrendingService {

    private final YouTube youtube;
    private final YoutubeRawRepository repository;

    @Value("${youtube.api.key1}") private String key1;
    @Value("${youtube.api.key2}") private String key2;
    @Value("${youtube.api.key3}") private String key3;
    @Value("${youtube.api.key4}") private String key4;

    /* 약 1 480 byte ─ 조사‧불용어‧단일 음절 300여 개 + 트렌드 키워드 */
    private static final String KO_JOSA_QUERY =
            /* ───────── 조사‧격조사 ───────── */
            "은|는|이|가|을|를|에|에서|에게|께|과|와|도|만|로|으로|밖에|부터|까지|이나|나|랑" + "|" +
                    /* ────── 접속사·의존명사·불용어 ────── */
                    "및|또는|그리고|그러나|하지만|그래서|때문에|그|것|때|듯|처럼|보다|보다도" + "|" +
                    /* ────── 불용어 동사·형용사 어간 ────── */
                    "하다|한다|하는|했다|했음|하고|하며|하기|되다|된다|되는|있다|없는|없다" + "|" +
                    /* ───────── 형식 명사 ───────── */
                    "사람|시간|방법|장소|이유|결과|내용|문제" + "|" +
                    /* ────── 단일·두 글자 음절 300여 개 ────── */
                    "가|각|간|갈|감|갑|강|개|객|거|걱|건|걸|검|겁|것|게|겐|겨|격|견|결|겸|겹|경|계|고|곡|곤|곧|"
                    + "골|곰|곱|곳|공|과|곽|관|괄|광|괴|굉|교|구|국|군|굴|굶|굼|굽|굿|궁|권|궐|귀|규|그|극|근|글|"
                    + "금|급|긋|긍|기|긴|길|김|깊|까|깍|깎|깐|깔|깜|깝|깡|깨|껍|꺼|꺾|껌|꼬|꼭|꽃|꼼|꼽|꽂|꽁|꽤|"
                    + "꾸|꾹|꾼|꿀|꿈|꿉|꿍|나|낙|난|날|남|납|낭|내|냄|냉|너|넉|넌|널|넘|넙|넥|넷|넹|노|녹|논|놀|"
                    + "놈|높|농|뇌|뇨|누|눅|눈|눌|늄|느|늑|늠|늦|니|닉|닌|닐|님|다|닥|단|달|담|답|당|대|댁|더|덕|"
                    + "던|덜|덤|덥|덩|데|덱|도|독|돈|돌|돔|돕|동|돼|되|된|될|됐|두|둑|둔|둘|둠|둡|둥|뒤|듀|드|득|"
                    + "든|들|듬|듭|등|디|딕|딘|딜|딥|딧|따|딱|딴|딸|땀|땅|때|떠|떡|떨|떼|또|뚜|뚝|뚫|뚱|뜨|뜩|"
                    + "뜬|뜰|뜸|뜹|띄|라|락|란|랄|람|랍|랑|래|략|러|럭|런|럴|럼|럽|렁|레|렉|렌|렐|렘|렵|렙|려|력|"
                    + "련|렬|렴|령|례|로|록|론|롤|롬|롭|롱|뢰|료|루|룩|룬|룰|룸|룹|룽|르|륙|른|를|름|릅|릇|릉|리|"
                    + "릭|린|릴|림|립|링|마|막|만|말|맘|맙|망|매|맥|맨|맬|맴|맵|맹|머|먹|먼|멀|멈|멍|메|멕|멘|멜|"
                    + "멤|멥|며|면|멸|명|몇|모|목|몬|몰|몸|몹|몽|무|묵|문|물|뭄|뭍|뭘|므|믄|믈|믐|믿|미|믹|민|"
                    + "밀|밈|밉|밍|바|박|반|발|밤|밥|방|배|백|밴|밸|뱀|뱁|뱃|뱅|버|벅|번|벌|범|법|벙|베|벡|벤|벨|"
                    + "벰|벱|벼|벽|변|별|볍|병|보|복|본|볼|봄|봇|봉|봐|뵙|부|북|분|불|붐|붑|붕|뷰|브|블|비|빅|빈|빌|"
                    + "빔|빕|빛|빙|빠|빡|빤|빨|빰|빱|빵|빼|뺨|뻐|뻔|뻘|뻥|뼈|뽀|뽕|뿌|뿐|쁘|삐|사|삭|산|살|삼|삽|"
                    + "상|새|색|샌|샘|샙|샤|샥|섀|서|석|선|설|섬|섭|성|세|섹|센|셀|셈|셉|셔|션|셜|셥|셰|소|속|손|솔|"
                    + "솜|솝|송|쇠|쇼|숀|수|숙|순|술|숨|숩|숭|쉬|스|습|승|시|식|신|실|심|십|싱|싸|싹|싼|쌀|쌈|쌉|"
                    + "쌍|써|썩|썬|썰|썼|쏘|쏙|쏠|쑤|쓰|쓴|쓸|씀|씹|아|악|안|알|암|압|앙|앞|애|액|앤|앨|앰|앱|야|"
                    + "약|언|얼|엄|업|없|엉|에|엑|엔|엘|엠|엡|여|역|연|열|염|엽|영|예|옛|오|옥|온|올|옮|옴|옵|옹|"
                    + "와|완|왕|왜|외|왼|요|욕|용|우|욱|운|울|움|웃|웅|워|원|월|웜|웹|웽|위|윅|윈|윌|윔|윙|유|육|"
                    + "윤|율|융|으|윽|은|을|음|읍|응|의|이|익|인|일|임|입|잇|잉|자|작|잔|잘|잠|잡|장|재|잭|젠|젤|"
                    + "젬|접|정|제|조|족|존|졸|좀|좁|종|좌|죄|주|죽|준|줄|줌|줍|중|쥐|쥔|즈|즉|즌|즐|즘|지|직|진|"
                    + "질|짐|집|징|짜|짝|짠|짧|짬|짭|짱|째|쩍|쩐|쩔|쩜|쪼|쫄|쯔|찌|찍|찐|찔|찜|찝|찡|차|착|찬|찰|"
                    + "참|찹|창|채|책|챈|챌|챔|챕|처|척|천|철|첨|첩|청|체|쳐|초|촉|촌|촐|총|최|쵸|추|축|춘|출|춤|"
                    + "춥|충|취|츠|측|치|칙|친|칠|침|칩|칭|카|칵|칸|칼|캄|캅|캉|캐|캑|캔|캘|캠|캡|커|컥|컨|컬|컴|컵|"
                    + "컹|케|켓|켄|켈|켬|켜|켤|코|콕|콘|콜|콤|콥|콩|쾌|쾨|쿄|쿠|쿡|쿤|쿨|쿰|쿱|쿵|크|큰|클|큼|키|킥|"
                    + "킨|킬|킴|킹|타|탁|탄|탈|탐|탑|탕|태|택|탠|탤|탬|탭|터|턱|턴|털|텀|텁|텅|테|텍|텐|텔|템|텝|티|"
                    + "틱|틴|틸|팀|팁|팅|파|팍|판|팔|팜|팝|팡|패|팩|팬|팰|팸|퍼|퍽|펀|펄|펌|펍|펑|페|펙|펜|펠|펨|펴|"
                    + "편|펼|평|폐|포|폭|폰|폴|폼|폽|표|푸|푹|풀|품|풍|프|플|픔|피|픽|핀|필|핌|핍|핑|하|학|한|할|함|"
                    + "합|항|해|핵|핸|햄|햅|허|헉|헌|헐|험|협|혁|혀|현|혈|형|혜|호|혹|혼|홀|홈|홉|홍|화|확|환|활|황|"
                    + "회|획|효|후|훅|훈|훌|훔|훗|흐|흑|흔|흘|흠|흡|흥|흰|히|힉|힌|힐|힘|힙|빙|퉁|랄|브|스|짤|썰|밈" + "|" +
                    /* ────── 트렌드·콘텐츠 키워드 ────── */
                    "레전드|꿀팁|실시간|생방송|다시보기|풀버전|하이라이트|직캠|먹방|브이로그|"
                    + "챌린지|유행|피셜|미쳤다|커버|라이브|공식|티저|예고편|리액션|리뷰|후기|분석|강의|"
                    + "튜토리얼|쉬운|초보|정보|썸네일|인기|핫클립|베스트|추천|쇼츠";


    /* 4 개의 키를 라운드로빈으로 사용 */
    private List<String> apiKeys;
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        this.apiKeys = List.of(key1, key2, key3, key4);
    }

    private String nextKey() {
        int idx = keyIndex.getAndUpdate(i -> (i + 1) % apiKeys.size());
        System.out.println("인덱스 : " + idx);
        return apiKeys.get(idx);
    }

    /**
     * 30분 간격 실행 전제 · 8페이지(최대 400 개) 수집
     */
    public void fetchAndSaveLastQuarter() {
        Instant now = Instant.now();

        String publishedAfter = now.minus(30, ChronoUnit.MINUTES).toString();
        LocalDateTime cutoff = LocalDateTime.ofInstant(now, ZoneId.of("Asia/Seoul"))
                .minusMinutes(30);

        LocalDateTime collectedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .truncatedTo(ChronoUnit.MINUTES);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String ts = collectedAt.format(fmt);          // ← 로그용 타임스탬프

        List<YoutubeRaw> batch = new ArrayList<>();
        String pageToken = null;      // 첫 페이지
        int pageLimit = 8;            // 8 페이지(≈ 400 개)

        for (int page = 0; page < pageLimit; page++) {
            try {
                SearchListResponse resp = youtube.search()
                        .list(List.of("snippet"))
                        .setKey(nextKey())                 // 키 라운드로빈
                        .setQ(KO_JOSA_QUERY)
                        .setType(List.of("video"))
                        .setOrder("date")
                        .setRegionCode("KR")
                        .setPublishedAfter(publishedAfter)
                        .setRelevanceLanguage("ko")
                        .setMaxResults(50L)
                        .setPageToken(pageToken)           // 다음 페이지
                        .execute();

                /* ① 페이지별 원본 개수 */
                int rawCnt = resp.getItems().size();
                System.out.printf("---[%s] [Page %d] raw items = %d%n", ts, page + 1, rawCnt);

                for (SearchResult item : resp.getItems()) {

                    // --- 업로드 시각을 LocalDateTime(서울)로 변환 ---
                    DateTime ytTime   = item.getSnippet().getPublishedAt();   // YouTube DateTime
                    Instant  instant  = Instant.ofEpochMilli(ytTime.getValue());
                    LocalDateTime uploaded = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Seoul"));


                    if (uploaded.isBefore(cutoff)) {      // 30분보다 이전이면
                        System.out.printf("---[%s] 30분 경계 도달 → 수집 종료%n", ts);
                        continue;                            // 처리 종료
                    }

                    var sn = item.getSnippet();
                    String title = sn.getTitle().replace("\n", " ");
                    String desc  = sn.getDescription().replace("\n", " ").trim();

                    /* ② 영상 제목·설명 한 줄 로그 */
                    System.out.printf("  - %s | %s%n", title, desc);

                    YoutubeRaw r = new YoutubeRaw();
                    r.setTime(collectedAt);
                    r.setTitle(title);
                    r.setDes(desc);
                    batch.add(r);
                }


                pageToken = resp.getNextPageToken();
                if (pageToken == null) {
                    /* ③ 다음 페이지 없음 */
                    System.out.printf("---[%s] 더 이상 페이지 없음%n", ts);
                    break;             // 더 이상 페이지 없음
                }
            }
            /* API 오류 처리 */
            catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
                System.err.printf("API 오류 %d : %s%n",
                        e.getDetails().getCode(), e.getDetails().getMessage());
                if ("quotaExceeded".equals(e.getDetails().getErrors()
                        .get(0).getReason())) {
                    // 다음 키로 바로 시도
                    continue;
                }
                break;
            }
            catch (java.io.IOException e) {
                System.err.println("네트워크 오류: " + e.getMessage());
                break;
            }
            catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        if (!batch.isEmpty()) {
            repository.saveAll(batch);
            System.out.printf("----------------------Saved %d Korean videos%n", batch.size());
        } else {
            System.out.println("----------------------수집된 한국어 영상이 없습니다.");
        }
    }
}
