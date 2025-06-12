package controllers;

import play.mvc.*;
import play.data.*;
import views.html.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.*;
import java.util.regex.Pattern;

import javax.inject.Inject;

/**
 * This controller contains actions to handle HTTP requests
 * for the fortune telling application.
 */
public class HomeController extends Controller {

    private final AssetsFinder assetsFinder;
    private final FormFactory formFactory;

    @Inject
    public HomeController(AssetsFinder assetsFinder, FormFactory formFactory) {
        this.assetsFinder = assetsFinder;
        this.formFactory = formFactory;
    }

    /**
     * An action that renders the fortune telling input form.
     */
    public Result index(Http.Request request) {
        return ok(
            index.render(
                "占いシステムへようこそ",
                request,
                assetsFinder
            ));
    }

    /**
     * An action that renders the four pillars fortune input form.
     */
    public Result fourPillarsInput(Http.Request request) {
        return ok(
            fourpillars_input.render(
                "四柱推命占いシステムへようこそ",
                request,
                assetsFinder
            ));
    }

    /**
     * An action that renders the name fortune input form.
     */
    public Result nameFortuneInput(Http.Request request) {
        return ok(
            name_fortune_input.render(
                "姓名判断システムへようこそ",
                request,
                assetsFinder
            ));
    }

    /**
     * An action that processes the fortune telling form and displays results.
     */
    public Result fortune(Http.Request request) {
        Form<FortuneData> form = formFactory.form(FortuneData.class).bindFromRequest(request);
        
        if (form.hasErrors()) {
            return badRequest(fourpillars_input.render("入力内容に誤りがあります", request, assetsFinder));
        }
        
        FortuneData data = form.get();
        
        // Generate fortune for the next 7 days
        Map<String, Map<String, String>> fortuneResults = generateWeeklyFortune(data);
        
        return ok(fortune.render(
            data.birthDate,
            data.birthTime != null ? data.birthTime : "",
            data.prefecture,
            data.gender,
            fortuneResults,
            assetsFinder
        ));
    }

    /**
     * An action that processes the name fortune form and displays results.
     */
    public Result nameFortune(Http.Request request) {
        Form<NameFortuneData> form = formFactory.form(NameFortuneData.class).bindFromRequest(request);
        
        if (form.hasErrors()) {
            return badRequest(name_fortune_input.render("入力内容に誤りがあります", request, assetsFinder));
        }
        
        NameFortuneData data = form.get();
        
        // Generate name fortune based on stroke counts
        Map<String, String> fortuneResults = generateNameFortune(data);
        
        return ok(name_fortune_result.render(
            data.familyName,
            data.givenName,
            data.gender,
            fortuneResults,
            assetsFinder
        ));
    }
    private Map<String, Map<String, String>> generateWeeklyFortune(FortuneData data) {
        Map<String, Map<String, String>> weeklyFortune = new LinkedHashMap<>();
        
        // Parse birth date for fortune calculation
        LocalDate birthDate = LocalDate.parse(data.birthDate);
        LocalDate today = LocalDate.now();
        
        // Calculate basic fortune elements based on birth date
        int birthSum = calculateBirthSum(birthDate);
        String[] elements = {"木", "火", "土", "金", "水"};
        String primaryElement = elements[birthSum % 5];
        
        // Generate fortune for next 7 days
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = today.plusDays(i);
            String dayName = getDayName(currentDate, i);
            
            Map<String, String> dailyFortune = generateDailyFortune(
                birthDate, currentDate, primaryElement, data.gender
            );
            
            weeklyFortune.put(dayName, dailyFortune);
        }
        
        return weeklyFortune;
    }

    /**
     * Calculate a simple numeric value from birth date for fortune generation.
     */
    private int calculateBirthSum(LocalDate birthDate) {
        return birthDate.getYear() + birthDate.getMonthValue() + birthDate.getDayOfMonth();
    }

    /**
     * Get formatted day name with date.
     */
    private String getDayName(LocalDate date, int dayOffset) {
        String[] weekDays = {"日", "月", "火", "水", "木", "金", "土"};
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int dayIndex = dayOfWeek.getValue() % 7; // Sunday = 0
        
        String dayName = weekDays[dayIndex];
        String dateStr = date.format(DateTimeFormatter.ofPattern("M月d日"));
        
        if (dayOffset == 0) {
            return "今日 (" + dateStr + " " + dayName + "曜日)";
        } else if (dayOffset == 1) {
            return "明日 (" + dateStr + " " + dayName + "曜日)";
        } else {
            return dateStr + " (" + dayName + "曜日)";
        }
    }

    /**
     * Generate daily fortune based on Four Pillars elements (simplified).
     */
    private Map<String, String> generateDailyFortune(
            LocalDate birthDate, LocalDate currentDate, String element, String gender
    ) {
        Map<String, String> fortune = new HashMap<>();
        
        // Simple fortune calculation based on date relationships
        int dayDiff = (int) (currentDate.toEpochDay() - birthDate.toEpochDay());
        int fortuneIndex = Math.abs(dayDiff + currentDate.getDayOfMonth()) % 4;
        
        String[] ratings = {"excellent", "good", "average", "poor"};
        String[] ratingTexts = {"大吉", "吉", "中吉", "小吉"};
        
        String rating = ratings[fortuneIndex];
        String ratingText = ratingTexts[fortuneIndex];
        
        fortune.put("rating", rating);
        fortune.put("ratingText", ratingText);
        
        // Generate specific fortunes based on rating
        switch (fortuneIndex) {
            case 0: // excellent
                fortune.put("overall", "運気が最高潮に達しています。積極的な行動で大きな成果が期待できるでしょう。");
                fortune.put("love", "恋愛面で嬉しい出来事が起こりそうです。告白や重要な話し合いに良い日です。");
                fortune.put("work", "仕事では大きなチャンスが訪れるかもしれません。自信を持って取り組みましょう。");
                fortune.put("health", "体調も良好で、エネルギーに満ちあふれています。");
                break;
            case 1: // good
                fortune.put("overall", "安定した運気で、計画していたことを進めるのに適しています。");
                fortune.put("love", "穏やかな恋愛運です。相手との関係を深めるのに良い日でしょう。");
                fortune.put("work", "着実に成果を上げることができそうです。丁寧な作業を心がけましょう。");
                fortune.put("health", "健康状態は良好です。適度な運動を取り入れるとさらに良いでしょう。");
                break;
            case 2: // average
                fortune.put("overall", "普通の運気です。無理をせず、現状維持を心がけると良いでしょう。");
                fortune.put("love", "恋愛面では特に大きな変化はありませんが、小さな心遣いが大切です。");
                fortune.put("work", "地道な努力が後々実を結ぶでしょう。焦らず着実に進めてください。");
                fortune.put("health", "体調管理に注意が必要です。十分な休息を取りましょう。");
                break;
            case 3: // poor
                fortune.put("overall", "少し運気が下がり気味です。新しいことは避け、慎重に行動しましょう。");
                fortune.put("love", "恋愛面では誤解が生じやすい日です。コミュニケーションを大切にしてください。");
                fortune.put("work", "仕事では思わぬトラブルが発生するかもしれません。冷静な対応を心がけましょう。");
                fortune.put("health", "疲れが溜まりやすい日です。無理をせず、早めの休息を取りましょう。");
                break;
        }
        
        return fortune;
    }

    /**
     * Generate name fortune based on stroke counts (simplified).
     */
    private Map<String, String> generateNameFortune(NameFortuneData data) {
        Map<String, String> fortune = new HashMap<>();
        
        // Calculate stroke counts
        int familyNameStrokes = calculateStrokes(data.familyName);
        int givenNameStrokes = calculateStrokes(data.givenName);
        
        // Calculate traditional name divination numbers
        int tenKaku = familyNameStrokes;  // Heaven number (姓の画数)
        int chiKaku = givenNameStrokes;   // Earth number (名の画数)
        int soKaku = familyNameStrokes + givenNameStrokes;  // Total number (総画数)
        
        // Simplified jinKaku calculation (person number)
        // In real name divination, this would be more complex
        int jinKaku = (tenKaku % 10) + (chiKaku % 10);
        if (jinKaku > 20) jinKaku = jinKaku - 10;
        
        // Store basic information
        fortune.put("familyNameStrokes", String.valueOf(familyNameStrokes));
        fortune.put("givenNameStrokes", String.valueOf(givenNameStrokes));
        fortune.put("tenKaku", String.valueOf(tenKaku));
        fortune.put("jinKaku", String.valueOf(jinKaku));
        fortune.put("chiKaku", String.valueOf(chiKaku));
        fortune.put("soKaku", String.valueOf(soKaku));
        
        // Generate fortune based on stroke counts
        generateStrokeFortune(fortune, soKaku, data.gender);
        
        return fortune;
    }
    
    /**
     * Calculate stroke count for Japanese characters (simplified).
     * In a real implementation, this would use a comprehensive stroke count database.
     */
    private int calculateStrokes(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // Basic stroke count mapping for common characters
        Map<Character, Integer> strokeMap = createStrokeMap();
        
        int totalStrokes = 0;
        for (char c : text.toCharArray()) {
            Integer strokes = strokeMap.get(c);
            if (strokes != null) {
                totalStrokes += strokes;
            } else {
                // Default stroke count for unmapped characters (based on complexity)
                if (isKanji(c)) {
                    totalStrokes += estimateKanjiStrokes(c);
                } else if (isHiragana(c)) {
                    totalStrokes += 3; // Average for hiragana
                } else if (isKatakana(c)) {
                    totalStrokes += 3; // Average for katakana
                } else {
                    totalStrokes += 1; // Default for other characters
                }
            }
        }
        
        return totalStrokes;
    }
    
    /**
     * Create a stroke count mapping for common Japanese characters.
     */
    private Map<Character, Integer> createStrokeMap() {
        Map<Character, Integer> map = new HashMap<>();
        
        // Common family name kanji
        map.put('田', 5); map.put('中', 4); map.put('佐', 7); map.put('藤', 18);
        map.put('山', 3); map.put('川', 3); map.put('木', 4); map.put('村', 7);
        map.put('高', 10); map.put('小', 3); map.put('松', 8); map.put('井', 4);
        map.put('石', 5); map.put('橋', 16); map.put('林', 8); map.put('森', 12);
        map.put('清', 11); map.put('水', 4); map.put('金', 8); map.put('銀', 14);
        map.put('鈴', 13); map.put('伊', 6); map.put('加', 5); map.put('近', 7);
        map.put('大', 3); map.put('西', 6); map.put('東', 8); map.put('南', 9);
        map.put('北', 5); map.put('上', 3); map.put('下', 3); map.put('前', 9);
        map.put('後', 9); map.put('原', 10); map.put('野', 11); map.put('島', 10);
        map.put('竹', 6); map.put('花', 7); map.put('草', 9); map.put('木', 4);
        
        // Common given name kanji
        map.put('太', 4); map.put('郎', 9); map.put('一', 1); map.put('二', 2);
        map.put('三', 3); map.put('四', 5); map.put('五', 4); map.put('六', 4);
        map.put('七', 2); map.put('八', 2); map.put('九', 2); map.put('十', 2);
        map.put('美', 9); map.put('子', 3); map.put('花', 7); map.put('香', 9);
        map.put('愛', 13); map.put('恵', 10); map.put('智', 12); map.put('理', 11);
        map.put('彩', 11); map.put('綾', 14); map.put('真', 10); map.put('純', 10);
        map.put('明', 8); map.put('光', 6); map.put('正', 5); map.put('和', 8);
        map.put('雄', 12); map.put('男', 7); map.put('夫', 4); map.put('人', 2);
        map.put('介', 4); map.put('助', 7); map.put('治', 8); map.put('次', 6);
        map.put('良', 7); map.put('優', 17); map.put('健', 11); map.put('強', 11);
        
        // Hiragana
        map.put('あ', 3); map.put('い', 2); map.put('う', 2); map.put('え', 2); map.put('お', 3);
        map.put('か', 3); map.put('き', 3); map.put('く', 2); map.put('け', 3); map.put('こ', 2);
        map.put('さ', 3); map.put('し', 3); map.put('す', 2); map.put('せ', 3); map.put('そ', 2);
        map.put('た', 4); map.put('ち', 3); map.put('つ', 3); map.put('て', 3); map.put('と', 2);
        map.put('な', 4); map.put('に', 3); map.put('ぬ', 2); map.put('ね', 4); map.put('の', 1);
        map.put('は', 3); map.put('ひ', 2); map.put('ふ', 4); map.put('へ', 1); map.put('ほ', 4);
        map.put('ま', 3); map.put('み', 3); map.put('む', 3); map.put('め', 2); map.put('も', 3);
        map.put('や', 3); map.put('ゆ', 2); map.put('よ', 3);
        map.put('ら', 2); map.put('り', 2); map.put('る', 2); map.put('れ', 2); map.put('ろ', 3);
        map.put('わ', 2); map.put('ゐ', 3); map.put('ゑ', 3); map.put('を', 3); map.put('ん', 1);
        
        // Katakana (basic mapping similar to hiragana)
        map.put('ア', 2); map.put('イ', 2); map.put('ウ', 3); map.put('エ', 3); map.put('オ', 3);
        map.put('カ', 3); map.put('キ', 3); map.put('ク', 2); map.put('ケ', 3); map.put('コ', 2);
        map.put('サ', 3); map.put('シ', 3); map.put('ス', 2); map.put('セ', 3); map.put('ソ', 2);
        map.put('タ', 3); map.put('チ', 3); map.put('ツ', 3); map.put('テ', 3); map.put('ト', 2);
        map.put('ナ', 2); map.put('ニ', 2); map.put('ヌ', 2); map.put('ネ', 4); map.put('ノ', 1);
        map.put('ハ', 3); map.put('ヒ', 2); map.put('フ', 4); map.put('ヘ', 1); map.put('ホ', 4);
        map.put('マ', 2); map.put('ミ', 3); map.put('ム', 2); map.put('メ', 2); map.put('モ', 3);
        map.put('ヤ', 3); map.put('ユ', 2); map.put('ヨ', 3);
        map.put('ラ', 2); map.put('リ', 2); map.put('ル', 2); map.put('レ', 2); map.put('ロ', 3);
        map.put('ワ', 2); map.put('ヰ', 3); map.put('ヱ', 3); map.put('ヲ', 3); map.put('ン', 1);
        
        return map;
    }
    
    /**
     * Check if character is kanji.
     */
    private boolean isKanji(char c) {
        return (c >= 0x4E00 && c <= 0x9FAF) || (c >= 0x3400 && c <= 0x4DBF);
    }
    
    /**
     * Check if character is hiragana.
     */
    private boolean isHiragana(char c) {
        return c >= 0x3040 && c <= 0x309F;
    }
    
    /**
     * Check if character is katakana.
     */
    private boolean isKatakana(char c) {
        return c >= 0x30A0 && c <= 0x30FF;
    }
    
    /**
     * Estimate stroke count for unmapped kanji characters.
     */
    private int estimateKanjiStrokes(char c) {
        // Very simplified estimation based on Unicode code point
        int codePoint = (int) c;
        if (codePoint < 0x5000) return 4;  // Simple kanji
        else if (codePoint < 0x7000) return 8;  // Medium complexity
        else if (codePoint < 0x8000) return 12; // Complex kanji
        else return 16; // Very complex kanji
    }
    
    /**
     * Generate fortune interpretations based on stroke counts.
     */
    private void generateStrokeFortune(Map<String, String> fortune, int totalStrokes, String gender) {
        // Determine fortune based on total strokes (simplified system)
        int fortuneIndex = totalStrokes % 4;
        
        String[] ratings = {"excellent", "good", "average", "poor"};
        String[] ratingTexts = {"大吉", "吉", "中吉", "小吉"};
        
        String rating = ratings[fortuneIndex];
        String ratingText = ratingTexts[fortuneIndex];
        
        // Apply same rating to all categories for simplicity
        fortune.put("overallRating", rating);
        fortune.put("overallRatingText", ratingText);
        fortune.put("loveRating", rating);
        fortune.put("loveRatingText", ratingText);
        fortune.put("workRating", rating);
        fortune.put("workRatingText", ratingText);
        fortune.put("healthRating", rating);
        fortune.put("healthRatingText", ratingText);
        
        // Generate fortune messages based on rating
        switch (fortuneIndex) {
            case 0: // excellent
                fortune.put("overallFortune", "画数から見て非常に良い運勢を持っています。積極的に行動することで大きな成果を得られるでしょう。");
                fortune.put("loveFortune", "恋愛面では非常に恵まれた運勢です。理想的な相手と出会える可能性が高いでしょう。");
                fortune.put("workFortune", "仕事運が非常に良く、昇進や成功のチャンスに恵まれます。リーダーシップを発揮しましょう。");
                fortune.put("healthFortune", "健康運も良好で、体力に恵まれています。スポーツなどで才能を発揮できるでしょう。");
                fortune.put("advice", "あなたの名前は非常に良い画数を持っています。自信を持って積極的に行動することで、人生の多くの分野で成功を収めることができるでしょう。");
                break;
            case 1: // good
                fortune.put("overallFortune", "安定した良い運勢を持っています。着実な努力により確実に成果を上げることができます。");
                fortune.put("loveFortune", "恋愛運は安定しており、誠実な関係を築くことができます。結婚運にも恵まれています。");
                fortune.put("workFortune", "仕事では堅実な成果を上げることができます。コツコツとした努力が報われるでしょう。");
                fortune.put("healthFortune", "健康面は安定しており、大きな病気の心配は少ないでしょう。規則正しい生活を心がけましょう。");
                fortune.put("advice", "あなたの名前は安定した良い画数を示しています。急がず着実に目標に向かって進むことで、確実に成功を手にすることができます。");
                break;
            case 2: // average
                fortune.put("overallFortune", "平均的な運勢ですが、努力次第で運勢を向上させることができます。諦めずに頑張りましょう。");
                fortune.put("loveFortune", "恋愛面では平凡ですが、相手を思いやる気持ちを大切にすることで良い関係を築けます。");
                fortune.put("workFortune", "仕事では地道な努力が必要ですが、継続することで必ず結果がついてきます。");
                fortune.put("healthFortune", "健康面では特に問題ありませんが、生活習慣に気をつけることが大切です。");
                fortune.put("advice", "あなたの名前は平均的な画数を持っています。運勢は努力次第で向上します。前向きな気持ちを持ち続けることが成功の鍵です。");
                break;
            case 3: // poor
                fortune.put("overallFortune", "やや困難な運勢ですが、困難を乗り越えることで大きく成長できます。諦めない心が大切です。");
                fortune.put("loveFortune", "恋愛面では試練がありますが、真の愛を見つけることで幸せを掴むことができます。");
                fortune.put("workFortune", "仕事では苦労することもありますが、その経験が将来の大きな財産となります。");
                fortune.put("healthFortune", "健康面では注意が必要です。ストレス管理と規則正しい生活を心がけましょう。");
                fortune.put("advice", "あなたの名前は試練を示す画数を持っていますが、それは成長の機会でもあります。困難に負けず、前向きに取り組むことで必ず道は開けます。");
                break;
        }
    }

    /**
     * Data binding class for the fortune telling form.
     */
    public static class FortuneData {
        public String birthDate;
        public String birthTime;
        public String prefecture;
        public String gender;
        
        // Getters and setters
        public String getBirthDate() { return birthDate; }
        public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
        
        public String getBirthTime() { return birthTime; }
        public void setBirthTime(String birthTime) { this.birthTime = birthTime; }
        
        public String getPrefecture() { return prefecture; }
        public void setPrefecture(String prefecture) { this.prefecture = prefecture; }
        
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
    }
    
    /**
     * Data binding class for the name fortune form.
     */
    public static class NameFortuneData {
        public String familyName;
        public String givenName;
        public String gender;
        
        // Getters and setters
        public String getFamilyName() { return familyName; }
        public void setFamilyName(String familyName) { this.familyName = familyName; }
        
        public String getGivenName() { return givenName; }
        public void setGivenName(String givenName) { this.givenName = givenName; }
        
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
    }
}
