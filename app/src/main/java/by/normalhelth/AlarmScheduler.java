package by.normalhelth;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/** Schedules the routine as local device alarms. No network or account is needed. */
final class AlarmScheduler {
    static final String KIND = "kind";
    static final String KIND_BEDTIME = "bedtime";
    static final String KIND_WAKE = "wake";
    static final String KIND_BREAKFAST = "breakfast";
    static final String KIND_LUNCH = "lunch";
    static final String KIND_DINNER = "dinner";
    static final String KIND_STRETCH = "stretch";
    static final String KIND_WALK = "walk";

    private static final int BEDTIME_ID = 101;
    private static final int WAKE_ID = 102;
    private static final int BREAKFAST_ID = 201;
    private static final int LUNCH_ID = 202;
    private static final int DINNER_ID = 203;
    private static final int STRETCH_START_ID = 300;
    private static final int WALK_START_ID = 400;

    private AlarmScheduler() { }

    static int bedtime(Context context) {
        int wake = AppPrefs.time(context, AppPrefs.WAKE_TIME, 7 * 60);
        int sleep = AppPrefs.get(context).getInt(AppPrefs.SLEEP_MINUTES, 8 * 60);
        int result = wake - sleep;
        while (result < 0) result += 24 * 60;
        return result % (24 * 60);
    }

    @SuppressLint("ScheduleExactAlarm")
    static void scheduleAll(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        cancelAll(context, alarmManager);
        android.content.SharedPreferences prefs = AppPrefs.get(context);

        if (prefs.getBoolean(AppPrefs.SLEEP_ENABLED, true)) {
            scheduleDaily(context, alarmManager, BEDTIME_ID, bedtime(context),
                    KIND_BEDTIME, "Пора готовиться ко сну", "Приглушите свет, отложите экран и начните спокойный вечерний ритуал.");
            scheduleDaily(context, alarmManager, WAKE_ID,
                    AppPrefs.time(context, AppPrefs.WAKE_TIME, 7 * 60),
                    KIND_WAKE, "Доброе утро", "Стакан воды, дневной свет и несколько мягких движений помогут начать день.");
        }

        if (prefs.getBoolean(AppPrefs.MEALS_ENABLED, true)) {
            scheduleDaily(context, alarmManager, BREAKFAST_ID,
                    AppPrefs.time(context, AppPrefs.BREAKFAST_TIME, 8 * 60),
                    KIND_BREAKFAST, "Время завтрака", "Выберите блюдо из вариантов в приложении и не торопитесь во время еды.");
            scheduleDaily(context, alarmManager, LUNCH_ID,
                    AppPrefs.time(context, AppPrefs.LUNCH_TIME, 13 * 60),
                    KIND_LUNCH, "Время обеда", "Откройте меню дня: здесь собраны домашние варианты белорусской кухни.");
            scheduleDaily(context, alarmManager, DINNER_ID,
                    AppPrefs.time(context, AppPrefs.DINNER_TIME, 19 * 60),
                    KIND_DINNER, "Время ужина", "Лёгкий ужин без спешки — хороший способ завершить питательную часть дня.");
        }

        if (prefs.getBoolean(AppPrefs.ACTIVITY_ENABLED, true)) {
            int interval = prefs.getInt(AppPrefs.ACTIVITY_INTERVAL, 60);
            if (interval != 45 && interval != 60 && interval != 90) interval = 60;
            int id = STRETCH_START_ID;
            for (int minute = 9 * 60; minute <= 20 * 60; minute += interval) {
                scheduleDaily(context, alarmManager, id++, minute, KIND_STRETCH,
                        "Время размяться", "Встаньте на 3 минуты: расправьте плечи, сделайте несколько шагов и мягко потянитесь.");
            }

            int goal = prefs.getInt(AppPrefs.WALK_GOAL, 2);
            int[] walkTimes = goal >= 3
                    ? new int[]{10 * 60 + 30, 14 * 60 + 30, 18 * 60 + 30}
                    : new int[]{12 * 60 + 30, 18 * 60 + 30};
            for (int i = 0; i < walkTimes.length; i++) {
                scheduleDaily(context, alarmManager, WALK_START_ID + i, walkTimes[i], KIND_WALK,
                        "Небольшая прогулка", "Если есть возможность, выйдите на 10–20 минут и смените обстановку.");
            }
        }
    }

    private static void cancelAll(Context context, AlarmManager alarmManager) {
        for (int id = 100; id < 500; id++) {
            PendingIntent pendingIntent = pendingIntent(context, id, null, false);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private static void scheduleDaily(Context context, AlarmManager alarmManager, int id, int minute,
                                      String kind, String title, String body) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, minute / 60);
        next.set(Calendar.MINUTE, minute % 60);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!isInFuture(next)) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(KIND, kind);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        PendingIntent pendingIntent = pendingIntent(context, id, intent, true);
        long triggerAt = next.getTimeInMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // The app remains useful when the user does not grant exact-alarm access.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    private static PendingIntent pendingIntent(Context context, int id, Intent source, boolean update) {
        Intent intent = source == null ? new Intent(context, ReminderReceiver.class) : source;
        int flags = PendingIntent.FLAG_IMMUTABLE;
        if (update) flags |= PendingIntent.FLAG_UPDATE_CURRENT;
        return PendingIntent.getBroadcast(context, id, intent, flags);
    }

    // Calendar has no readable "is after now" convenience method on all API levels.
    private static boolean isInFuture(Calendar calendar) {
        return calendar.getTimeInMillis() > System.currentTimeMillis();
    }
}
