package by.normalhelth;

import android.content.Context;
import android.content.SharedPreferences;

/** Small, dependency-free settings store for the daily routine. */
final class AppPrefs {
    private static final String FILE = "normal_helth_preferences";

    static final String WAKE_TIME = "wake_time";
    static final String SLEEP_MINUTES = "sleep_minutes";
    static final String BREAKFAST_TIME = "breakfast_time";
    static final String LUNCH_TIME = "lunch_time";
    static final String DINNER_TIME = "dinner_time";
    static final String SLEEP_ENABLED = "sleep_enabled";
    static final String MEALS_ENABLED = "meals_enabled";
    static final String ACTIVITY_ENABLED = "activity_enabled";
    static final String ACTIVITY_INTERVAL = "activity_interval";
    static final String WALK_GOAL = "walk_goal";
    static final String MENU_DAY = "menu_day";
    static final String MENU_HISTORY = "menu_history";
    static final String STRETCH_DONE = "stretch_done";
    static final String WALK_DONE = "walk_done";
    static final String ACTIVITY_DAY = "activity_day";

    private AppPrefs() { }

    static SharedPreferences get(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static int time(Context context, String key, int defaultMinutes) {
        return get(context).getInt(key, defaultMinutes);
    }

    static String timeLabel(int minutes) {
        int normalized = ((minutes % 1440) + 1440) % 1440;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", normalized / 60, normalized % 60);
    }

    static String dayKey() {
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        return format.format(new java.util.Date());
    }

    static void resetActivityIfNeeded(Context context) {
        SharedPreferences prefs = get(context);
        String today = dayKey();
        if (!today.equals(prefs.getString(ACTIVITY_DAY, ""))) {
            prefs.edit()
                    .putString(ACTIVITY_DAY, today)
                    .putBoolean(STRETCH_DONE, false)
                    .putBoolean(WALK_DONE, false)
                    .apply();
        }
    }
}
