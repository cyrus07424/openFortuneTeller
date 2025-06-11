package controllers;

import play.mvc.*;
import play.data.*;
import views.html.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.*;

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
                "四柱推命占いシステムへようこそ",
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
            return badRequest(index.render("入力内容に誤りがあります", request, assetsFinder));
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
     * Generate weekly fortune data based on Four Pillars principles (simplified).
     */
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
}
