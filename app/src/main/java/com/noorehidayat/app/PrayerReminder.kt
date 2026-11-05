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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal object PrayerReminderScheduler {
    const val CHANNEL_ID = "noor_prayer_reminders"
    private const val ACTION_NOTIFY = "com.noorehidayat.app.PRAYER_NOTIFY"
    private const val ACTION_REFRESH = "com.noorehidayat.app.PRAYER_REFRESH"
    private val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    fun scheduleDay(context: Context, timings: Map<String, String>) {
        val prefs = context.getSharedPreferences("noor_prefs", 0)
        if (!prefs.getBoolean("prayer_reminders_enabled", false)) return
        createChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        prayerNames.forEachIndexed { index, name ->
            val pending = notificationIntent(context, name, 4100 + index)
            alarmManager.cancel(pending)
            val raw = timings[name] ?: return@forEachIndexed
            val parts = raw.substringBefore(" ").split(":")
            if (parts.size < 2) return@forEachIndexed
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 0)
                set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis > now) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
            }
        }
        scheduleNextRefresh(context)
    }

    fun scheduleNextRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val intent = Intent(context, PrayerReminderReceiver::class.java).setAction(ACTION_REFRESH)
        val pending = PendingIntent.getBroadcast(context, 4999, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
    }

    fun refreshFromSavedLocation(context: Context) {
        val prefs = context.getSharedPreferences("noor_prefs", 0)
        if (!prefs.getBoolean("prayer_reminders_enabled", false)) return
        val lat = prefs.getString("prayer_latitude", null)?.toDoubleOrNull() ?: return
        val lon = prefs.getString("prayer_longitude", null)?.toDoubleOrNull() ?: return
        val method = prefs.getInt("prayer_method", 1)
        val school = prefs.getInt("prayer_school", 1)
        val date = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
        val connection = URL("https://api.aladhan.com/v1/timings/$date?latitude=$lat&longitude=$lon&method=$method&school=$school").openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        try {
            if (connection.responseCode !in 200..299) return
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (root.optInt("code") != 200) return
            val timings = root.getJSONObject("data").getJSONObject("timings")
            scheduleDay(context, prayerNames.associateWith { timings.optString(it) })
        } finally {
            connection.disconnect()
        }
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Prayer reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifications at the daily prayer times you enable"
            })
        }
    }

    private fun notificationIntent(context: Context, name: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, PrayerReminderReceiver::class.java).setAction(ACTION_NOTIFY).putExtra("prayer_name", name)
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    internal fun showNotification(context: Context, name: String) {
        createChannel(context)
        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = openApp?.let { PendingIntent.getActivity(context, 5001, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$name prayer time")
            .setContentText("It is time for $name. Take a moment for prayer.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(6000 + prayerNames.indexOf(name).coerceAtLeast(0), notification)
        }
    }
}

class PrayerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.noorehidayat.app.PRAYER_NOTIFY" -> {
                if (context.getSharedPreferences("noor_prefs", 0).getBoolean("prayer_reminders_enabled", false)) {
                    PrayerReminderScheduler.showNotification(context, intent.getStringExtra("prayer_name") ?: "Prayer")
                }
            }
            "com.noorehidayat.app.PRAYER_REFRESH" -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try { PrayerReminderScheduler.refreshFromSavedLocation(context) }
                    catch (_: Exception) { PrayerReminderScheduler.scheduleNextRefresh(context) }
                    finally { pending.finish() }
                }
            }
        }
    }
}

class PrayerReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { PrayerReminderScheduler.refreshFromSavedLocation(context) }
            catch (_: Exception) { PrayerReminderScheduler.scheduleNextRefresh(context) }
            finally { pending.finish() }
        }
    }
}
