package com.noorehidayat.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

internal const val DAILY_REMINDER_CHANNEL = "noor_daily_islamic_reminders"
private const val DAILY_REMINDER_ACTION = "com.noorehidayat.app.DAILY_REMINDER"

internal fun dailyReminderMessage(dayOfYear: Int): String {
    val messages = listOf(
        "Take a few minutes for Quran recitation today.",
        "Make time for dhikr and dua today.",
        "Reflect on the meaning of one Quran ayah today.",
        "Pause, be grateful, and remember Allah today.",
        "Set aside a quiet moment for prayer and reflection."
    )
    return messages[Math.floorMod(dayOfYear, messages.size)]
}

internal object DailyReminderScheduler {
    private const val REQUEST_CODE = 7201

    fun schedule(context: Context) {
        if (!context.getSharedPreferences("noor_prefs", 0).getBoolean("daily_reminder_enabled", false)) return
        createChannel(context)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent(context))
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).setAction(DAILY_REMINDER_ACTION)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(DAILY_REMINDER_CHANNEL, "Daily Islamic reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Optional daily prompts for Quran recitation, dhikr and reflection"
                }
            )
        }
    }

    fun notifyAndReschedule(context: Context) {
        if (!context.getSharedPreferences("noor_prefs", 0).getBoolean("daily_reminder_enabled", false)) return
        createChannel(context)
        val message = dailyReminderMessage(Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(context, 7202, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(context, DAILY_REMINDER_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Noor-e-Hidayat · Daily reminder")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(7203, notification)
        }
        schedule(context)
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DailyReminderScheduler.notifyAndReschedule(context)
    }
}
