package by.normalhelth;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The main screen of ZI-Normal-Helth. It intentionally uses only Android SDK widgets so the
 * APK stays small, works offline, and does not need a third-party account.
 */
public class MainActivity extends android.app.Activity {
    private static final int SCREEN_HOME = 0;
    private static final int SCREEN_NUTRITION = 1;
    private static final int SCREEN_ACTIVITY = 2;
    private static final int SCREEN_SETTINGS = 3;
    private static final int NOTIFICATION_REQUEST = 71;

    private static final int BG = Color.rgb(245, 248, 244);
    private static final int SURFACE = Color.WHITE;
    private static final int INK = Color.rgb(32, 53, 46);
    private static final int MUTED = Color.rgb(107, 125, 118);
    private static final int SAGE = Color.rgb(47, 107, 83);
    private static final int SAGE_DARK = Color.rgb(31, 77, 59);
    private static final int SAGE_LIGHT = Color.rgb(225, 238, 229);
    private static final int PEACH_LIGHT = Color.rgb(255, 240, 229);
    private static final int LAVENDER_LIGHT = Color.rgb(240, 238, 251);
    private static final int LINE = Color.rgb(229, 236, 231);

    private SharedPreferences prefs;
    private FrameLayout screenHolder;
    private LinearLayout bottomNavigation;
    private final List<LinearLayout> navItems = new ArrayList<>();
    private final List<TextView> navIcons = new ArrayList<>();
    private final List<TextView> navTitles = new ArrayList<>();
    private int currentScreen = SCREEN_HOME;

    private static class Meal {
        final String key;
        final String title;
        final String emoji;
        final int defaultTime;
        final String tip;
        final String[] ids;
        final String[] dishes;

        Meal(String key, String title, String emoji, int defaultTime, String tip,
             String[] ids, String[] dishes) {
            this.key = key;
            this.title = title;
            this.emoji = emoji;
            this.defaultTime = defaultTime;
            this.tip = tip;
            this.ids = ids;
            this.dishes = dishes;
        }
    }

    private Meal[] meals() {
        return new Meal[]{
                new Meal(
                        "breakfast", "Завтрак", "☀", 8 * 60,
                        "Сочетайте белок, цельную крупу или хлеб, фрукт или ягоды и немного полезных жиров. Сахар и жарку используйте умеренно.",
                        new String[]{"buckwheat_berries", "cottage_berry_nuts", "omelet_greens", "oat_apple_flax"},
                        new String[]{
                                "Гречневая каша с молоком, ягодами и льняным семенем",
                                "Творог с яблоком, ягодами и небольшим количеством орехов",
                                "Омлет с зеленью и овощами, хлеб из цельного зерна",
                                "Овсяная каша с яблоком, семенами льна и натуральным йогуртом"
                        }),
                new Meal(
                        "lunch", "Обед", "◒", 13 * 60,
                        "Выберите одно основное блюдо, добавьте салат и при желании небольшую порцию супа. Вода — рядом.",
                        new String[]{"baked_draniki", "grechaniki_cabbage", "fish_grecha_beet", "holubtsy_grechka"},
                        new String[]{
                                "Драники, запечённые в духовке, с курицей и йогуртовым соусом",
                                "Гречаники с тушёной капустой и морковью",
                                "Запечённая рыба с гречкой, свёклой и зеленью",
                                "Голубцы с мясом и гречкой"
                        }),
                new Meal(
                        "dinner", "Ужин", "☾", 19 * 60,
                        "Лёгкий ужин: белок, овощи и небольшая порция крупы или бобовых. Витамины лучше получать из разнообразной еды.",
                        new String[]{"cabbage_beans", "fish_barley_cabbage", "potato_mushrooms_egg", "lentils_vegetables"},
                        new String[]{
                                "Тушёная капуста с фасолью, морковью и зеленью",
                                "Запечённая рыба с перловкой и салатом из капусты",
                                "Картофель с грибами, яйцом и овощным салатом",
                                "Чечевица с тушёными овощами и цельнозерновым хлебом"
                        })
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(SURFACE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        prefs = AppPrefs.get(this);
        AppPrefs.resetActivityIfNeeded(this);
        AlarmScheduler.scheduleAll(this);
        buildShell();
        showScreen(SCREEN_HOME);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs != null && screenHolder != null) {
            AppPrefs.resetActivityIfNeeded(this);
            showScreen(currentScreen);
        }
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        screenHolder = new FrameLayout(this);
        screenHolder.setBackgroundColor(BG);
        root.addView(screenHolder, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        buildBottomNavigation();
        root.addView(bottomNavigation, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(76)));

        // Android 15 draws app content edge-to-edge by default. Reserve the
        // navigation-bar inset so the three-button/gesture area never covers
        // the labels of the app's own bottom navigation.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                int bottomInset;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                } else {
                    bottomInset = insets.getSystemWindowInsetBottom();
                }
                LinearLayout.LayoutParams navigationParams =
                        (LinearLayout.LayoutParams) bottomNavigation.getLayoutParams();
                int desiredHeight = dp(76) + bottomInset;
                if (navigationParams.height != desiredHeight) {
                    navigationParams.height = desiredHeight;
                    bottomNavigation.setLayoutParams(navigationParams);
                }
                bottomNavigation.setPadding(dp(10), dp(7), dp(10), dp(7) + bottomInset);
                return insets;
            });
        }
        setContentView(root);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) root.requestApplyInsets();
    }

    private void buildBottomNavigation() {
        bottomNavigation = new LinearLayout(this);
        bottomNavigation.setOrientation(LinearLayout.HORIZONTAL);
        bottomNavigation.setGravity(Gravity.CENTER_VERTICAL);
        bottomNavigation.setPadding(dp(10), dp(7), dp(10), dp(7));
        bottomNavigation.setBackgroundColor(SURFACE);

        addNavItem(0, "⌂", "Главная");
        addNavItem(1, "◌", "Питание");
        addNavItem(2, "↗", "Активность");
        addNavItem(3, "⚙", "Настройки");
    }

    private void addNavItem(final int index, String icon, String title) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(8), dp(4), dp(8), dp(4));
        item.setContentDescription(title);
        item.setOnClickListener(v -> showScreen(index));

        TextView iconView = text(icon, 22, MUTED, true);
        iconView.setGravity(Gravity.CENTER);
        TextView titleView = text(title, 11, MUTED, false);
        titleView.setGravity(Gravity.CENTER);
        item.addView(iconView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));
        item.addView(titleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(20)));
        bottomNavigation.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
        navItems.add(item);
        navIcons.add(iconView);
        navTitles.add(titleView);
    }

    private void showScreen(int screen) {
        currentScreen = screen;
        screenHolder.removeAllViews();
        View view;
        if (screen == SCREEN_NUTRITION) view = nutritionScreen();
        else if (screen == SCREEN_ACTIVITY) view = activityScreen();
        else if (screen == SCREEN_SETTINGS) view = settingsScreen();
        else view = homeScreen();
        screenHolder.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        updateNavigation();
    }

    private void updateNavigation() {
        for (int i = 0; i < navItems.size(); i++) {
            boolean selected = i == currentScreen;
            navItems.get(i).setBackground(round(selected ? SAGE_LIGHT : Color.TRANSPARENT, 18));
            navIcons.get(i).setTextColor(selected ? SAGE : MUTED);
            navTitles.get(i).setTextColor(selected ? SAGE_DARK : MUTED);
            navTitles.get(i).setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private View page(LinearLayout content) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(BG);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(8), 0, dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private void addHeader(LinearLayout content, String eyebrow, String title, String subtitle) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(16), dp(20), dp(13));
        TextView overline = text(eyebrow.toUpperCase(new Locale("ru")), 11, SAGE, true);
        overline.setLetterSpacing(0.08f);
        header.addView(overline, wrap());
        TextView heading = text(title, 30, INK, true);
        heading.setPadding(0, dp(4), 0, 0);
        header.addView(heading, wrap());
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView sub = text(subtitle, 14, MUTED, false);
            sub.setPadding(0, dp(5), 0, 0);
            header.addView(sub, wrap());
        }
        content.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private View homeScreen() {
        LinearLayout content = new LinearLayout(this);
        View page = page(content);
        addHeader(content, "ZI-Normal-Helth", "Баланс дня", dateCaption());
        content.addView(homeHero(), cardParams(0, 8, 0, 8));
        content.addView(sleepCard(), cardParams(0, 8, 0, 8));
        content.addView(morningCard(), cardParams(0, 8, 0, 8));
        content.addView(homeActivityCard(), cardParams(0, 8, 0, 8));
        return page;
    }

    private View homeHero() {
        LinearLayout card = card(SAGE);
        card.setPadding(dp(20), dp(19), dp(20), dp(20));

        LinearLayout top = row();
        TextView label = text("СЕГОДНЯ", 12, Color.WHITE, true);
        label.setLetterSpacing(0.08f);
        top.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView badge = pill("В РИТМЕ", Color.rgb(76, 132, 105), Color.WHITE);
        top.addView(badge, wrap());
        card.addView(top, wrap());

        card.addView(text("Следующее событие", 13, Color.rgb(205, 229, 211), false), topMargin(18));
        LinearLayout next = row();
        TextView nextTime = text(nextEventTime(), 34, Color.WHITE, true);
        next.addView(nextTime, wrap());
        TextView nextName = text("  ·  " + nextEventName(), 14, Color.rgb(228, 242, 230), false);
        nextName.setGravity(Gravity.BOTTOM);
        next.addView(nextName, new LinearLayout.LayoutParams(0, dp(38), 1f));
        card.addView(next, wrap());
        TextView note = text("Сон, питание и движение — маленькими шагами без перегрузки.", 14,
                Color.rgb(220, 239, 224), false);
        note.setPadding(0, dp(8), 0, 0);
        card.addView(note, wrap());
        return card;
    }

    private View sleepCard() {
        LinearLayout card = card(SURFACE);
        LinearLayout titleRow = row();
        titleRow.addView(text("Сон и отдых", 19, INK, true), new LinearLayout.LayoutParams(0, -2, 1f));
        titleRow.addView(pill("НАСТРОЕНО", SAGE_LIGHT, SAGE), wrap());
        card.addView(titleRow, wrap());
        card.addView(text("Будильник и напоминание об отбое рассчитываются от времени подъёма.",
                13, MUTED, false), topMargin(7));

        LinearLayout times = row();
        times.setPadding(0, dp(18), 0, dp(15));
        times.setBaselineAligned(false);
        LinearLayout wake = metricColumn("Подъём", AppPrefs.timeLabel(AppPrefs.time(this, AppPrefs.WAKE_TIME, 7 * 60)));
        LinearLayout bed = metricColumn("Ложиться", AppPrefs.timeLabel(AlarmScheduler.bedtime(this)));
        LinearLayout.LayoutParams wakeParams = new LinearLayout.LayoutParams(0, -2, 1f);
        wakeParams.rightMargin = dp(12);
        LinearLayout.LayoutParams bedParams = new LinearLayout.LayoutParams(0, -2, 1f);
        bedParams.leftMargin = dp(12);
        times.addView(wake, wakeParams);
        times.addView(bed, bedParams);
        card.addView(times, wrap());
        card.addView(divider(), wrap());
        LinearLayout bottom = row();
        int duration = prefs.getInt(AppPrefs.SLEEP_MINUTES, 8 * 60);
        bottom.addView(text("Цель сна  ·  " + durationLabel(duration), 13, MUTED, false),
                new LinearLayout.LayoutParams(0, -2, 1f));
        TextView edit = linkButton("Изменить");
        edit.setOnClickListener(v -> showScreen(SCREEN_SETTINGS));
        bottom.addView(edit, wrap());
        card.addView(bottom, topMargin(14));
        return card;
    }

    private LinearLayout metricColumn(String label, String value) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView labelView = text(label, 12, MUTED, false);
        labelView.setSingleLine(true);
        TextView valueView = text(value, 25, INK, true);
        valueView.setSingleLine(true);
        column.addView(labelView, wrap());
        column.addView(valueView, topMargin(4));
        return column;
    }

    private View morningCard() {
        LinearLayout card = card(SURFACE);
        card.addView(text("Мягкое начало", 19, INK, true), wrap());
        card.addView(text("Три простые опоры после подъёма", 13, MUTED, false), topMargin(5));
        addRoutineRow(card, "01", "Свет и вода", "Откройте окно или выйдите к дневному свету");
        addRoutineRow(card, "02", "Разминка", "Три минуты мягких движений для тела");
        addRoutineRow(card, "03", "Завтрак", "Спокойно выберите блюдо из меню дня");

        LinearLayout exercise = new LinearLayout(this);
        exercise.setOrientation(LinearLayout.VERTICAL);
        exercise.setPadding(dp(14), dp(14), dp(14), dp(14));
        exercise.setBackground(round(SAGE_LIGHT, 18));
        LinearLayout exerciseTop = row();
        exerciseTop.addView(text("Упражнение для утра", 16, SAGE_DARK, true),
                new LinearLayout.LayoutParams(0, -2, 1f));
        exerciseTop.addView(pill("2 МИН/ШАГ", Color.WHITE, SAGE), wrap());
        exercise.addView(exerciseTop, wrap());
        exercise.addView(text("Пять понятных этапов: стойка и дыхание, потягивание, плечи, повороты корпуса и шаги на месте.",
                13, MUTED, false), topMargin(7));
        exercise.addView(text("Каждый этап длится 2 минуты. Двигайтесь без рывков и остановитесь, если появляется боль или головокружение.",
                12, MUTED, false), topMargin(5));
        TextView guide = actionButton("Запустить: картинки + диктор", false);
        guide.setOnClickListener(v -> showMorningSteps());
        exercise.addView(guide, topMargin(11));
        LinearLayout.LayoutParams exerciseParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        exerciseParams.topMargin = dp(16);
        card.addView(exercise, exerciseParams);

        card.addView(text("Пошаговая анимация с AI-диктором", 16, INK, true), topMargin(17));
        ImageView sequence = new ImageView(this);
        sequence.setImageResource(R.drawable.morning_sequence);
        sequence.setScaleType(ImageView.ScaleType.FIT_CENTER);
        sequence.setAdjustViewBounds(true);
        sequence.setClickable(true);
        sequence.setContentDescription("Нажмите, чтобы запустить пять картинок по очереди с голосовой подсказкой");
        sequence.setOnClickListener(v -> showMorningSteps());
        LinearLayout.LayoutParams sequenceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(156));
        sequenceParams.topMargin = dp(7);
        card.addView(sequence, sequenceParams);
        card.addView(text("Нажмите на картинки или кнопку выше: каждый этап длится 2 минуты, AI-диктор объясняет движение и считает повторы.",
                12, MUTED, false), topMargin(5));
        return card;
    }

    private void addRoutineRow(LinearLayout card, String number, String title, String subtitle) {
        LinearLayout item = row();
        item.setPadding(0, dp(15), 0, 0);
        TextView numberView = pill(number, SAGE_LIGHT, SAGE);
        numberView.setGravity(Gravity.CENTER);
        item.addView(numberView, fixed(40, 30));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), 0, 0, 0);
        copy.addView(text(title, 15, INK, true), wrap());
        copy.addView(text(subtitle, 12, MUTED, false), topMargin(3));
        item.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(item, wrap());
    }

    private View homeActivityCard() {
        LinearLayout card = card(SURFACE);
        LinearLayout top = row();
        top.addView(text("Активность", 19, INK, true), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView open = linkButton("Открыть");
        open.setOnClickListener(v -> showScreen(SCREEN_ACTIVITY));
        top.addView(open, wrap());
        card.addView(top, wrap());
        boolean stretch = prefs.getBoolean(AppPrefs.STRETCH_DONE, false);
        boolean walk = prefs.getBoolean(AppPrefs.WALK_DONE, false);
        card.addView(text((stretch ? "✓ Разминка отмечена" : "○ Разминка впереди")
                        + "    " + (walk ? "✓ Прогулка отмечена" : "○ Прогулка впереди"),
                13, MUTED, false), topMargin(9));
        return card;
    }

    private View nutritionScreen() {
        ensureMenuForToday();
        LinearLayout content = new LinearLayout(this);
        View page = page(content);
        addHeader(content, "ПИТАНИЕ", "Меню дня", "Домашние варианты с привычными продуктами Беларуси");

        LinearLayout summary = card(SAGE_DARK);
        summary.setPadding(dp(19), dp(18), dp(19), dp(18));
        LinearLayout summaryTop = row();
        summaryTop.addView(text("Три приёма пищи", 18, Color.WHITE, true),
                new LinearLayout.LayoutParams(0, -2, 1f));
        TextView day = pill("НА СЕГОДНЯ", Color.rgb(62, 103, 83), Color.rgb(221, 241, 225));
        summaryTop.addView(day, wrap());
        summary.addView(summaryTop, wrap());
        summary.addView(text("Отмечайте то, что планируете съесть. Так проще замечать разнообразие по дням.",
                13, Color.rgb(219, 236, 222), false), topMargin(7));
        TextView generate = actionButton("Сформировать меню", true);
        generate.setOnClickListener(v -> {
            generateMenuInternal(true);
            showScreen(SCREEN_NUTRITION);
        });
        summary.addView(generate, topMargin(15));
        content.addView(summary, cardParams(0, 8, 0, 8));

        for (Meal meal : meals()) {
            content.addView(mealCard(meal), cardParams(0, 8, 0, 8));
        }

        LinearLayout advice = card(Color.rgb(250, 246, 237));
        advice.addView(text("Небольшая подсказка", 16, INK, true), wrap());
        advice.addView(text("Пейте воду в течение дня и ориентируйтесь на голод и насыщение. Приложение не заменяет индивидуальные рекомендации врача.",
                13, MUTED, false), topMargin(6));
        content.addView(advice, cardParams(0, 8, 0, 8));

        LinearLayout vitamins = card(SAGE_LIGHT);
        vitamins.addView(text("Питательные акценты", 17, INK, true), wrap());
        vitamins.addView(text("Ягоды, капуста и зелень помогают разнообразить источники витамина C; морковь и зелёные овощи — провитамина A; рыба, яйца и молочные продукты — белка, кальция и витамина B12. Рыба и лён добавляют полезные жиры.",
                13, MUTED, false), topMargin(6));
        vitamins.addView(text("Витамин D и любые добавки требуют индивидуальной оценки — их лучше согласовать с врачом.",
                13, MUTED, false), topMargin(7));
        content.addView(vitamins, cardParams(0, 8, 0, 8));
        return page;
    }

    private View mealCard(Meal meal) {
        LinearLayout card = card(SURFACE);
        LinearLayout title = row();
        TextView icon = text(meal.emoji, 24, SAGE, false);
        icon.setGravity(Gravity.CENTER);
        title.addView(icon, fixed(38, 38));
        LinearLayout titleCopy = new LinearLayout(this);
        titleCopy.setOrientation(LinearLayout.VERTICAL);
        titleCopy.setPadding(dp(10), 0, 0, 0);
        titleCopy.addView(text(meal.title, 19, INK, true), wrap());
        TextView time = pill(AppPrefs.timeLabel(timeForMeal(meal)), SAGE_LIGHT, SAGE);
        time.setOnClickListener(v -> openMealTimePicker(meal));
        titleCopy.addView(text("Нажмите на время, чтобы изменить", 12, MUTED, false), topMargin(3));
        title.addView(titleCopy, new LinearLayout.LayoutParams(0, -2, 1f));
        title.addView(time, wrap());
        card.addView(title, wrap());
        card.addView(text(meal.tip, 13, MUTED, false), topMargin(12));
        if ("lunch".equals(meal.key)) {
            card.addView(text("ОСНОВНОЕ БЛЮДО · выберите один вариант", 11, SAGE, true), topMargin(14));
        }

        for (int i = 0; i < meal.dishes.length; i++) {
            final String dishId = meal.ids[i];
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(meal.dishes[i]);
            checkBox.setTextSize(14);
            checkBox.setTextColor(INK);
            checkBox.setGravity(Gravity.CENTER_VERTICAL);
            checkBox.setMinHeight(dp(45));
            checkBox.setPadding(0, dp(3), 0, dp(3));
            checkBox.setButtonTintList(checkTint());
            checkBox.setChecked(prefs.getBoolean("dish_" + dishId, false));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                    prefs.edit().putBoolean("dish_" + dishId, isChecked).apply());
            card.addView(checkBox, topMargin(5));
        }
        if ("lunch".equals(meal.key)) addLunchComplements(card);
        return card;
    }

    private void addLunchComplements(LinearLayout card) {
        View separator = new View(this);
        separator.setBackgroundColor(LINE);
        LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        separatorParams.topMargin = dp(13);
        card.addView(separator, separatorParams);

        card.addView(text("Соберите полноценный обед", 17, INK, true), topMargin(14));
        card.addView(text("Схема на выбор: одно основное блюдо + салат; суп — по аппетиту небольшой порцией.",
                13, MUTED, false), topMargin(5));

        card.addView(text("Суп · по желанию", 13, SAGE, true), topMargin(13));
        addLunchComplement(card, "lunch_soup_shchi",
                "Щи из свежей капусты с зеленью", false);
        addLunchComplement(card, "lunch_soup_svekolnik",
                "Свекольник с кефиром и зеленью", false);

        card.addView(text("Салат · выберите один", 13, SAGE, true), topMargin(10));
        addLunchComplement(card, "lunch_salad_cabbage",
                "Салат из капусты, моркови и зелени", true);
        addLunchComplement(card, "lunch_salad_beet",
                "Свекольный салат с яблоком", false);
        addLunchComplement(card, "lunch_salad_cucumber",
                "Огурцы с зеленью и натуральным йогуртом", false);
        addLunchComplement(card, "lunch_water",
                "Вода или несладкий напиток", true);
    }

    private void addLunchComplement(LinearLayout card, String id, String label, boolean defaultChecked) {
        String key = "dish_" + id;
        if (!prefs.contains(key)) prefs.edit().putBoolean(key, defaultChecked).apply();
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(label);
        checkBox.setTextSize(14);
        checkBox.setTextColor(INK);
        checkBox.setGravity(Gravity.CENTER_VERTICAL);
        checkBox.setMinHeight(dp(42));
        checkBox.setPadding(0, dp(3), 0, dp(3));
        checkBox.setButtonTintList(checkTint());
        checkBox.setChecked(prefs.getBoolean(key, defaultChecked));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(key, isChecked).apply());
        card.addView(checkBox, topMargin(3));
    }

    private View activityScreen() {
        AppPrefs.resetActivityIfNeeded(this);
        LinearLayout content = new LinearLayout(this);
        View page = page(content);
        addHeader(content, "АКТИВНОСТЬ", "Движение в течение дня", "Короткие паузы, разминки и прогулки без гонки за результатом");

        LinearLayout hero = card(Color.rgb(224, 239, 227));
        hero.setPadding(dp(19), dp(18), dp(19), dp(18));
        hero.addView(text("Тело любит регулярность", 21, SAGE_DARK, true), wrap());
        hero.addView(text("Поставьте напоминания на удобный интервал и отмечайте выполненное.",
                13, SAGE_DARK, false), topMargin(6));
        content.addView(hero, cardParams(0, 8, 0, 8));

        LinearLayout progressCard = card(SURFACE);
        LinearLayout progressTop = row();
        progressTop.addView(text("План на сегодня", 19, INK, true), new LinearLayout.LayoutParams(0, -2, 1f));
        int completed = (prefs.getBoolean(AppPrefs.STRETCH_DONE, false) ? 1 : 0)
                + (prefs.getBoolean(AppPrefs.WALK_DONE, false) ? 1 : 0);
        progressTop.addView(pill(completed + " из 2", SAGE_LIGHT, SAGE), wrap());
        progressCard.addView(progressTop, wrap());
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(2);
        progress.setProgress(completed);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            progress.setProgressTintList(ColorStateList.valueOf(SAGE));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));
        }
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        progressParams.topMargin = dp(13);
        progressCard.addView(progress, progressParams);
        CheckBox stretch = activityCheck("Разминка", "3 минуты мягких движений", AppPrefs.STRETCH_DONE, false);
        CheckBox walk = activityCheck("Прогулка", "10–20 минут на свежем воздухе", AppPrefs.WALK_DONE, true);
        progressCard.addView(stretch, topMargin(10));
        progressCard.addView(walk, topMargin(3));
        content.addView(progressCard, cardParams(0, 8, 0, 8));

        LinearLayout reminderCard = card(SURFACE);
        LinearLayout rTop = row();
        LinearLayout rCopy = new LinearLayout(this);
        rCopy.setOrientation(LinearLayout.VERTICAL);
        rCopy.addView(text("Напоминания о разминке", 17, INK, true), wrap());
        rCopy.addView(text("В рабочее время, с 09:00 до 20:00", 12, MUTED, false), topMargin(3));
        rTop.addView(rCopy, new LinearLayout.LayoutParams(0, -2, 1f));
        Switch activitySwitch = settingSwitch(prefs.getBoolean(AppPrefs.ACTIVITY_ENABLED, true));
        rTop.addView(activitySwitch, wrap());
        activitySwitch.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(AppPrefs.ACTIVITY_ENABLED, checked).apply();
            AlarmScheduler.scheduleAll(this);
        });
        reminderCard.addView(rTop, wrap());
        reminderCard.addView(text("Интервал между разминками", 13, MUTED, false), topMargin(17));
        reminderCard.addView(chipRow(new String[]{"45 мин", "60 мин", "90 мин"},
                new int[]{45, 60, 90}, prefs.getInt(AppPrefs.ACTIVITY_INTERVAL, 60), value -> {
                    prefs.edit().putInt(AppPrefs.ACTIVITY_INTERVAL, value).apply();
                    AlarmScheduler.scheduleAll(this);
                    showScreen(SCREEN_ACTIVITY);
                }), topMargin(8));
        reminderCard.addView(divider(), topMargin(17));
        reminderCard.addView(text("Цель прогулок", 13, MUTED, false), topMargin(15));
        reminderCard.addView(chipRow(new String[]{"2 раза", "3 раза"}, new int[]{2, 3},
                prefs.getInt(AppPrefs.WALK_GOAL, 2), value -> {
                    prefs.edit().putInt(AppPrefs.WALK_GOAL, value).apply();
                    AlarmScheduler.scheduleAll(this);
                    showScreen(SCREEN_ACTIVITY);
                }), topMargin(8));
        content.addView(reminderCard, cardParams(0, 8, 0, 8));

        LinearLayout tip = card(LAVENDER_LIGHT);
        tip.addView(text("Если долго сидите", 16, INK, true), wrap());
        tip.addView(text("Поставьте стопы на пол, опустите плечи, посмотрите вдаль и сделайте несколько спокойных вдохов. Движение не должно причинять боль.",
                13, MUTED, false), topMargin(6));
        content.addView(tip, cardParams(0, 8, 0, 8));
        return page;
    }

    private CheckBox activityCheck(String title, String subtitle, String key, boolean walk) {
        CheckBox box = new CheckBox(this);
        box.setText(title + "\n" + subtitle);
        box.setTextSize(14);
        box.setTextColor(INK);
        box.setMinHeight(dp(52));
        box.setButtonTintList(checkTint());
        box.setChecked(prefs.getBoolean(key, false));
        box.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            if (walk) prefs.edit().putBoolean(AppPrefs.WALK_DONE, checked).apply();
            else prefs.edit().putBoolean(AppPrefs.STRETCH_DONE, checked).apply();
            showScreen(SCREEN_ACTIVITY);
        });
        return box;
    }

    private View settingsScreen() {
        LinearLayout content = new LinearLayout(this);
        View page = page(content);
        addHeader(content, "НАСТРОЙКИ", "Ваш режим", "Настройте время, а приложение напомнит о важных точках дня");

        LinearLayout sleep = card(SURFACE);
        sleep.addView(text("Сон и подъём", 19, INK, true), wrap());
        sleep.addView(settingRow("Время подъёма", "Будильник и утреннее напоминание",
                AppPrefs.timeLabel(AppPrefs.time(this, AppPrefs.WAKE_TIME, 7 * 60)),
                v -> openWakeTimePicker()), topMargin(12));
        sleep.addView(settingRow("Желаемая длительность сна", "От неё рассчитывается время отбоя",
                durationLabel(prefs.getInt(AppPrefs.SLEEP_MINUTES, 8 * 60)),
                v -> openSleepDurationDialog()), topMargin(7));
        sleep.addView(settingRow("Напоминание об отбое", "Вечерний ритуал без резкого будильника",
                AppPrefs.timeLabel(AlarmScheduler.bedtime(this)), null), topMargin(7));
        content.addView(sleep, cardParams(0, 8, 0, 8));

        LinearLayout notifications = card(SURFACE);
        notifications.addView(text("Напоминания", 19, INK, true), wrap());
        notifications.addView(text("Можно включать и выключать каждый блок отдельно.", 13, MUTED, false), topMargin(5));
        addSwitchSetting(notifications, "Сон и утро", "Отбой и время подъёма", AppPrefs.SLEEP_ENABLED,
                topMargin(14));
        addSwitchSetting(notifications, "Питание", "Завтрак, обед и ужин", AppPrefs.MEALS_ENABLED,
                topMargin(5));
        addSwitchSetting(notifications, "Активность", "Разминки и прогулки", AppPrefs.ACTIVITY_ENABLED,
                topMargin(5));
        content.addView(notifications, cardParams(0, 8, 0, 8));

        LinearLayout access = card(SURFACE);
        access.addView(text("Доступ к уведомлениям", 19, INK, true), wrap());
        boolean notificationsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                || ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)) == null
                || ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).areNotificationsEnabled();
        access.addView(text(notificationsAllowed
                        ? "Уведомления разрешены — расписание будет работать в фоне."
                        : "Разрешите уведомления, чтобы получать напоминания вовремя.",
                13, MUTED, false), topMargin(6));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            TextView permission = actionButton("Разрешить уведомления", false);
            permission.setOnClickListener(v -> requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST));
            access.addView(permission, topMargin(12));
        } else if (!notificationsAllowed) {
            TextView permission = actionButton("Открыть настройки уведомлений", false);
            permission.setOnClickListener(v -> openNotificationSettings());
            access.addView(permission, topMargin(12));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean exact = ((android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE))
                    .canScheduleExactAlarms();
            access.addView(text(exact
                            ? "Точные напоминания доступны."
                            : "Для точного времени можно разрешить будильники в системе.",
                    13, MUTED, false), topMargin(14));
            if (!exact) {
                TextView alarmPermission = actionButton("Разрешить точное время", false);
                alarmPermission.setOnClickListener(v -> openExactAlarmSettings());
                access.addView(alarmPermission, topMargin(10));
            }
        }
        content.addView(access, cardParams(0, 8, 0, 8));

        LinearLayout info = card(PEACH_LIGHT);
        info.addView(text("О приложении", 17, INK, true), wrap());
        info.addView(text("ZI-Normal-Helth помогает выстроить спокойную ежедневную последовательность: сон, питание и движение. Все данные хранятся только на этом устройстве.",
                13, MUTED, false), topMargin(6));
        content.addView(info, cardParams(0, 8, 0, 8));
        return page;
    }

    private void addSwitchSetting(LinearLayout card, String title, String subtitle,
                                  String key, ViewGroup.LayoutParams params) {
        LinearLayout row = row();
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(title, 15, INK, true), wrap());
        copy.addView(text(subtitle, 12, MUTED, false), topMargin(3));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        Switch toggle = settingSwitch(prefs.getBoolean(key, true));
        row.addView(toggle, wrap());
        toggle.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            AlarmScheduler.scheduleAll(this);
        });
        card.addView(row, params);
    }

    private View settingRow(String title, String subtitle, String value, View.OnClickListener listener) {
        LinearLayout row = row();
        row.setPadding(dp(2), dp(10), dp(2), dp(10));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(title, 15, INK, true), wrap());
        copy.addView(text(subtitle, 12, MUTED, false), topMargin(3));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView valueView = pill(value, SAGE_LIGHT, SAGE);
        row.addView(valueView, wrap());
        if (listener != null) {
            row.setClickable(true);
            row.setOnClickListener(listener);
            valueView.setOnClickListener(listener);
        }
        return row;
    }

    private void openWakeTimePicker() {
        int current = AppPrefs.time(this, AppPrefs.WAKE_TIME, 7 * 60);
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hour, minute) -> {
            prefs.edit().putInt(AppPrefs.WAKE_TIME, hour * 60 + minute).apply();
            AlarmScheduler.scheduleAll(this);
            showScreen(SCREEN_SETTINGS);
            Toast.makeText(this, "Время подъёма сохранено", Toast.LENGTH_SHORT).show();
        }, current / 60, current % 60, true);
        dialog.setTitle("Время подъёма");
        dialog.show();
    }

    private void openMealTimePicker(Meal meal) {
        int current = timeForMeal(meal);
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hour, minute) -> {
            prefs.edit().putInt(mealTimeKey(meal), hour * 60 + minute).apply();
            AlarmScheduler.scheduleAll(this);
            showScreen(SCREEN_NUTRITION);
            Toast.makeText(this, meal.title + ": время сохранено", Toast.LENGTH_SHORT).show();
        }, current / 60, current % 60, true);
        dialog.setTitle(meal.title);
        dialog.show();
    }

    private void openSleepDurationDialog() {
        final String[] labels = {"7 часов", "7 часов 30 минут", "8 часов", "8 часов 30 минут", "9 часов"};
        final int[] values = {7 * 60, 7 * 60 + 30, 8 * 60, 8 * 60 + 30, 9 * 60};
        int current = prefs.getInt(AppPrefs.SLEEP_MINUTES, 8 * 60);
        int selected = 2;
        for (int i = 0; i < values.length; i++) if (values[i] == current) selected = i;
        final int checked = selected;
        new AlertDialog.Builder(this)
                .setTitle("Желаемая длительность сна")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    prefs.edit().putInt(AppPrefs.SLEEP_MINUTES, values[which]).apply();
                    AlarmScheduler.scheduleAll(this);
                    dialog.dismiss();
                    showScreen(SCREEN_SETTINGS);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showMorningSteps() {
        final int[] images = {
                R.drawable.morning_exercise_1,
                R.drawable.morning_exercise_2,
                R.drawable.morning_exercise_3,
                R.drawable.morning_exercise_4,
                R.drawable.morning_exercise_5
        };
        final int[] explanations = {
                R.raw.morning_step_1_intro,
                R.raw.morning_step_2_intro,
                R.raw.morning_step_3_intro,
                R.raw.morning_step_4_intro,
                R.raw.morning_step_5_intro
        };
        final String[] titles = {
                "Стойка и дыхание",
                "Потягивание вверх",
                "Круги плечами назад",
                "Повороты корпуса",
                "Шаги на месте"
        };
        final String[] descriptions = {
                "Начало: стопы на ширине таза, руки свободно опущены. Конец: вернитесь в ту же ровную стойку.",
                "Начало: руки вдоль тела. Конец: плавно опустите руки и расслабьте плечи.",
                "Начало: ладони на плечах. Конец: локти опущены, шея расслаблена.",
                "Начало: руки скрещены на груди. Конец: вернитесь в центр, стопы остаются на месте.",
                "Начало: обе стопы на полу. Конец: поставьте обе стопы и спокойно остановите шаг."
        };
        final long exerciseDuration = 2L * 60L * 1000L;
        final long countInterval = 20L * 1000L;
        final Dialog dialog = new Dialog(this);
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(18), dp(16), dp(18), dp(16));
        sheet.setBackground(round(SURFACE, 24));

        LinearLayout titleRow = row();
        titleRow.addView(text("Разминка — 10 минут", 20, INK, true),
                new LinearLayout.LayoutParams(0, -2, 1f));
        TextView close = linkButton("Закрыть");
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, wrap());
        sheet.addView(titleRow, wrap());
        sheet.addView(text("Пять упражнений по 2 минуты. Первая панель показывает начало, последняя — завершение движения.",
                13, MUTED, false), topMargin(4));
        TextView audioStatus = text("🎙 AI-диктор: подготовка…", 12, SAGE, true);
        audioStatus.setGravity(Gravity.CENTER);
        sheet.addView(audioStatus, topMargin(7));

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackground(round(SAGE_LIGHT, 18));
        image.setContentDescription("Пять последовательных картинок текущего упражнения: начало, движение и завершение");
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(225));
        imageParams.topMargin = dp(13);
        sheet.addView(image, imageParams);

        TextView stepTitle = text("", 19, INK, true);
        stepTitle.setGravity(Gravity.CENTER);
        sheet.addView(stepTitle, topMargin(11));
        TextView explanation = text("", 13, MUTED, false);
        explanation.setGravity(Gravity.CENTER);
        sheet.addView(explanation, topMargin(4));

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            progress.setProgressTintList(ColorStateList.valueOf(SAGE));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));
        }
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        progressParams.topMargin = dp(13);
        sheet.addView(progress, progressParams);

        TextView timeStatus = text("Подготовка упражнения…", 13, SAGE_DARK, true);
        timeStatus.setGravity(Gravity.CENTER);
        sheet.addView(timeStatus, topMargin(8));
        sheet.addView(text("Диктор объясняет движение, затем считает по пять повторов. После пятого упражнения голос замолкает.",
                12, MUTED, false), topMargin(6));

        final Handler handler = new Handler(Looper.getMainLooper());
        final int[] current = {0};
        final long[] startedAt = {0L};
        final long[] nextCountAt = {0L};
        final boolean[] active = {true};
        final boolean[] exerciseRunning = {false};
        final boolean[] completed = {false};
        final MediaPlayer[] player = {null};
        final Runnable[] beginStep = new Runnable[1];
        final Runnable[] ticker = new Runnable[1];

        class AudioController {
            void release() {
                if (player[0] != null) {
                    try {
                        player[0].stop();
                    } catch (IllegalStateException ignored) {
                    }
                    player[0].release();
                    player[0] = null;
                }
            }

            void play(int resourceId, Runnable after) {
                release();
                MediaPlayer media = MediaPlayer.create(MainActivity.this, resourceId);
                if (media == null) {
                    audioStatus.setText("🔈 Не удалось запустить AI-диктора — инструкция остаётся на экране");
                    if (after != null && active[0]) after.run();
                    return;
                }
                player[0] = media;
                media.setOnCompletionListener(completedPlayer -> {
                    if (player[0] == completedPlayer) player[0] = null;
                    completedPlayer.release();
                    if (after != null && active[0]) after.run();
                });
                media.setOnErrorListener((errorPlayer, what, extra) -> {
                    if (player[0] == errorPlayer) player[0] = null;
                    errorPlayer.release();
                    audioStatus.setText("🔈 Аудио недоступно — инструкция остаётся на экране");
                    if (after != null && active[0]) after.run();
                    return true;
                });
                media.start();
            }
        }
        AudioController audio = new AudioController();

        beginStep[0] = () -> {
            if (!active[0]) return;
            exerciseRunning[0] = false;
            startedAt[0] = 0L;
            nextCountAt[0] = 0L;
            image.setImageResource(images[current[0]]);
            stepTitle.setText((current[0] + 1) + ". " + titles[current[0]]);
            explanation.setText(descriptions[current[0]]);
            progress.setProgress(0);
            timeStatus.setText("Сначала объяснение движения…");
            audioStatus.setText("🎙 AI-диктор объясняет");
            audio.play(explanations[current[0]], () -> {
                if (!active[0]) return;
                exerciseRunning[0] = true;
                startedAt[0] = android.os.SystemClock.uptimeMillis();
                nextCountAt[0] = countInterval;
                timeStatus.setText("Осталось 2:00");
                audioStatus.setText("🎙 AI-диктор считает повторы");
                audio.play(R.raw.morning_count, null);
            });
        };

        ticker[0] = () -> {
            if (!dialog.isShowing() || !active[0] || completed[0]) return;
            if (exerciseRunning[0]) {
                long elapsed = android.os.SystemClock.uptimeMillis() - startedAt[0];
                long remaining = Math.max(0L, exerciseDuration - elapsed);
                int value = (int) Math.min(1000L, elapsed * 1000L / exerciseDuration);
                progress.setProgress(value);
                timeStatus.setText(String.format(Locale.getDefault(), "Осталось %d:%02d",
                        remaining / 60000L, (remaining / 1000L) % 60L));
                if (elapsed >= exerciseDuration) {
                    audio.release();
                    exerciseRunning[0] = false;
                    progress.setProgress(1000);
                    if (current[0] + 1 < images.length) {
                        current[0]++;
                        beginStep[0].run();
                    } else {
                        completed[0] = true;
                        timeStatus.setText("Все пять упражнений завершены");
                        audioStatus.setText("🎙 AI-диктор завершает инструкцию");
                        audio.play(R.raw.morning_complete, () -> {
                            if (active[0]) audioStatus.setText("✓ Разминка окончена, диктор замолчал");
                        });
                    }
                } else if (elapsed >= nextCountAt[0] && player[0] == null) {
                    audio.play(R.raw.morning_count, null);
                    nextCountAt[0] += countInterval;
                }
            }
            handler.postDelayed(ticker[0], 250L);
        };

        image.setImageResource(images[0]);
        stepTitle.setText("1. " + titles[0]);
        explanation.setText(descriptions[0]);
        dialog.setContentView(sheet);
        dialog.setOnDismissListener(d -> {
            active[0] = false;
            handler.removeCallbacks(ticker[0]);
            audio.release();
        });
        dialog.show();
        handler.post(ticker[0]);
        audio.play(R.raw.morning_intro, beginStep[0]);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(24), dp(430));
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
    }

    private void openExactAlarmSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception ignored) {
            Toast.makeText(this, "Откройте настройки будильников для приложения вручную", Toast.LENGTH_LONG).show();
        }
    }

    private ColorStateList checkTint() {
        return new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{SAGE, Color.rgb(160, 176, 168)});
    }

    private Switch settingSwitch(boolean checked) {
        Switch toggle = new Switch(this);
        toggle.setChecked(checked);
        toggle.setShowText(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toggle.setThumbTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{Color.WHITE, Color.rgb(235, 241, 237)}));
            toggle.setTrackTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{SAGE, Color.rgb(190, 205, 197)}));
        }
        return toggle;
    }

    private LinearLayout chipRow(String[] labels, int[] values, int selected, ValueListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < labels.length; i++) {
            final int value = values[i];
            TextView chip = pill(labels[i], selected == value ? SAGE : SAGE_LIGHT,
                    selected == value ? Color.WHITE : SAGE);
            chip.setGravity(Gravity.CENTER);
            chip.setOnClickListener(v -> listener.onValue(value));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
            if (i > 0) lp.setMargins(dp(6), 0, 0, 0);
            row.addView(chip, lp);
        }
        return row;
    }

    private interface ValueListener {
        void onValue(int value);
    }

    private void ensureMenuForToday() {
        String today = AppPrefs.dayKey();
        if (!today.equals(prefs.getString(AppPrefs.MENU_DAY, ""))) {
            generateMenuInternal(false);
        }
    }

    private void generateMenuInternal(boolean showToast) {
        Meal[] mealList = meals();
        Set<String> recent = recentMenuIds();
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_YEAR) + calendar.get(Calendar.YEAR) * 2;
        StringBuilder chosen = new StringBuilder();
        for (int m = 0; m < mealList.length; m++) {
            Meal meal = mealList[m];
            int index = (day + m * 2) % meal.ids.length;
            for (int offset = 0; offset < meal.ids.length; offset++) {
                int candidate = (index + offset) % meal.ids.length;
                if (!recent.contains(meal.ids[candidate]) || offset == meal.ids.length - 1) {
                    index = candidate;
                    break;
                }
            }
            for (String id : meal.ids) prefs.edit().putBoolean("dish_" + id, false).apply();
            prefs.edit().putBoolean("dish_" + meal.ids[index], true).apply();
            if (chosen.length() > 0) chosen.append(",");
            chosen.append(meal.ids[index]);
        }
        String oldHistory = prefs.getString(AppPrefs.MENU_HISTORY, "");
        String[] entries = oldHistory.isEmpty() ? new String[0] : oldHistory.split("\\|");
        StringBuilder history = new StringBuilder();
        int start = Math.max(0, entries.length - 6);
        for (int i = start; i < entries.length; i++) {
            if (history.length() > 0) history.append("|");
            history.append(entries[i]);
        }
        if (history.length() > 0) history.append("|");
        history.append(AppPrefs.dayKey()).append(":").append(chosen);
        prefs.edit().putString(AppPrefs.MENU_DAY, AppPrefs.dayKey())
                .putString(AppPrefs.MENU_HISTORY, history.toString()).apply();
        if (showToast) Toast.makeText(this, "Меню обновлено с учётом недавних блюд", Toast.LENGTH_SHORT).show();
    }

    private Set<String> recentMenuIds() {
        Set<String> result = new HashSet<>();
        String history = prefs.getString(AppPrefs.MENU_HISTORY, "");
        if (history.isEmpty()) return result;
        String[] entries = history.split("\\|");
        int start = Math.max(0, entries.length - 3);
        for (int i = start; i < entries.length; i++) {
            int separator = entries[i].indexOf(':');
            if (separator >= 0 && separator + 1 < entries[i].length()) {
                String[] ids = entries[i].substring(separator + 1).split(",");
                for (String id : ids) result.add(id);
            }
        }
        return result;
    }

    private String mealTimeKey(Meal meal) {
        if ("breakfast".equals(meal.key)) return AppPrefs.BREAKFAST_TIME;
        if ("lunch".equals(meal.key)) return AppPrefs.LUNCH_TIME;
        return AppPrefs.DINNER_TIME;
    }

    private int timeForMeal(Meal meal) {
        return AppPrefs.time(this, mealTimeKey(meal), meal.defaultTime);
    }

    private String dateCaption() {
        SimpleDateFormat format = new SimpleDateFormat("EEEE, d MMMM", new Locale("ru"));
        String value = format.format(Calendar.getInstance().getTime());
        return value.substring(0, 1).toUpperCase(new Locale("ru")) + value.substring(1);
    }

    private String nextEventTime() {
        int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60
                + Calendar.getInstance().get(Calendar.MINUTE);
        int[] times = {
                AppPrefs.time(this, AppPrefs.WAKE_TIME, 7 * 60),
                AppPrefs.time(this, AppPrefs.BREAKFAST_TIME, 8 * 60),
                AppPrefs.time(this, AppPrefs.LUNCH_TIME, 13 * 60),
                AppPrefs.time(this, AppPrefs.DINNER_TIME, 19 * 60),
                AlarmScheduler.bedtime(this)
        };
        int best = 24 * 60;
        for (int time : times) {
            int delta = time - now;
            if (delta <= 0) delta += 24 * 60;
            if (delta < best) best = delta;
        }
        int next = (now + best) % (24 * 60);
        return AppPrefs.timeLabel(next);
    }

    private String nextEventName() {
        int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60
                + Calendar.getInstance().get(Calendar.MINUTE);
        String[] names = {"подъём", "завтрак", "обед", "ужин", "подготовка ко сну"};
        int[] times = {
                AppPrefs.time(this, AppPrefs.WAKE_TIME, 7 * 60),
                AppPrefs.time(this, AppPrefs.BREAKFAST_TIME, 8 * 60),
                AppPrefs.time(this, AppPrefs.LUNCH_TIME, 13 * 60),
                AppPrefs.time(this, AppPrefs.DINNER_TIME, 19 * 60),
                AlarmScheduler.bedtime(this)
        };
        int best = 24 * 60;
        int bestIndex = 0;
        for (int i = 0; i < times.length; i++) {
            int delta = times[i] - now;
            if (delta <= 0) delta += 24 * 60;
            if (delta < best) {
                best = delta;
                bestIndex = i;
            }
        }
        return names[bestIndex];
    }

    private String durationLabel(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        if (rest == 0) return hours + " ч";
        return hours + " ч " + rest + " мин";
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setIncludeFontPadding(true);
        return view;
    }

    private TextView pill(String value, int background, int color) {
        TextView view = text(value, 11, color, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(11), dp(7), dp(11), dp(7));
        view.setBackground(round(background, 18));
        return view;
    }

    private TextView actionButton(String value, boolean filled) {
        TextView view = text(value, 14, filled ? Color.WHITE : SAGE, true);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(44));
        view.setPadding(dp(16), dp(8), dp(16), dp(8));
        view.setBackground(round(filled ? SAGE : SAGE_LIGHT, 16));
        view.setClickable(true);
        return view;
    }

    private TextView linkButton(String value) {
        TextView view = text(value, 13, SAGE, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), dp(5), dp(4), dp(5));
        view.setClickable(true);
        return view;
    }

    private LinearLayout card(int background) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(round(background, 22));
        card.setElevation(dp(1));
        return card;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private View divider() {
        View divider = new View(this);
        divider.setBackgroundColor(LINE);
        return divider;
    }

    private GradientDrawable round(int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams fixed(int width, int height) {
        return new LinearLayout.LayoutParams(dp(width), dp(height));
    }

    private LinearLayout.LayoutParams topMargin(int top) {
        LinearLayout.LayoutParams params = wrap();
        params.topMargin = dp(top);
        return params;
    }

    private LinearLayout.LayoutParams cardParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(left + 16), dp(top), dp(right + 16), dp(bottom));
        return params;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onBackPressed() {
        if (currentScreen != SCREEN_HOME) showScreen(SCREEN_HOME);
        else super.onBackPressed();
    }
}
