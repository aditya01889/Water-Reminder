package keyur.diwan.project.waterReminder.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import keyur.diwan.project.waterReminder.recievers.BootReceiver
import keyur.diwan.project.waterReminder.recievers.NotifierReceiver
import java.util.concurrent.TimeUnit

class AlarmHelper {
    private var alarmManager: AlarmManager? = null
    private val ACTION_BD_NOTIFICATION: String = "io.github.z3r0c00l_2k.aquadroid.NOTIFICATION"

    fun setAlarm(context: Context, notificationFrequency: Long) {
        val notificationFrequencyMs = TimeUnit.MINUTES.toMillis(notificationFrequency)
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, NotifierReceiver::class.java).apply {
            action = ACTION_BD_NOTIFICATION
        }

        /* Adjust flags based on the Android version */
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingAlarmIntent = PendingIntent.getBroadcast(
            context,
            0,
            alarmIntent,
            flag
        )

        Log.i("AlarmHelper", "Setting Alarm Interval to: $notificationFrequency minutes")

        alarmManager!!.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            notificationFrequencyMs,
            pendingAlarmIntent
        )

        // Restart if rebooted
        val receiver = ComponentName(context, BootReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun cancelAlarm(context: Context) {
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val alarmIntent = Intent(context, NotifierReceiver::class.java).apply {
            action = ACTION_BD_NOTIFICATION
        }

        // Adjust flags based on the Android version
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingAlarmIntent = PendingIntent.getBroadcast(
            context,
            0,
            alarmIntent,
            flag
        )

        alarmManager!!.cancel(pendingAlarmIntent)

        // Alarm won't start again if device is rebooted
        val receiver = ComponentName(context, BootReceiver::class.java)
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        Log.i("AlarmHelper", "Cancelling alarms")
    }

    fun checkAlarm(context: Context): Boolean {
        val alarmIntent = Intent(context, NotifierReceiver::class.java).apply {
            action = ACTION_BD_NOTIFICATION
        }

        // Adjust flags based on the Android version
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        return PendingIntent.getBroadcast(
            context, 0,
            alarmIntent,
            flag
        ) != null
    }
}