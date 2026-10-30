package net.azterisk.weighttracker;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public class NotificationHelper {
    public static final String CHANNEL_ID = "weight_goal_channel";
    public static final String REMINDER_CHANNEL_ID = "weight_reminder_channel";
    public static final int NOTIFICATION_ID = 1001;
    public static final int REMINDER_NOTIFICATION_ID = 1002;

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager == null) return;

            // Goal channel
            NotificationChannel goalChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Goal Achievement Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            goalChannel.setDescription("Notifications when you reach your target weight goal");
            notificationManager.createNotificationChannel(goalChannel);

            // Daily reminder channel
            NotificationChannel reminderChannel = new NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Daily Weigh-In Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            reminderChannel.setDescription("Daily reminders to log your weight");
            notificationManager.createNotificationChannel(reminderChannel);
        }
    }

    public static void sendGoalNotification(Context context, String message) {
        createNotificationChannels(context);

        Intent intent = new Intent(context, WeightListActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Goal Achieved!")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {
            // Permission missing on Android 13+
        }
    }

    public static void sendDailyReminder(Context context) {
        createNotificationChannels(context);

        Intent intent = new Intent(context, AddWeightActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Morning Check-In")
                .setContentText("Step on the scale to log your daily progress!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(REMINDER_NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {
            // Permission missing on Android 13+
        }
    }

    public static void scheduleDailyReminder(Context context, boolean enable) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REMINDER_NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (enable) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 8);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }
}
