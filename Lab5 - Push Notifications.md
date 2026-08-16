# Lab 5: Implement reminders and push notifications for specific events

Complete step-by-step guide.

## Lab Overview
To implement exact time reminders in Android, you must use AlarmManager alongside NotificationManager. While WorkManager is used for deferrable background work (like periodic syncing in Lab 3), it cannot trigger at exact timestamps. AlarmManager is the correct API for time-sensitive alarms.

This lab covers creating an event, scheduling an exact alarm, and displaying a push notification when the alarm triggers.

## Step 1: Add Permissions and Register Receivers
Open AndroidManifest.xml and add the following above the application tag:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

Next, register the BroadcastReceivers inside the application tag. These will listen for the alarm trigger and device reboots:

```xml
<receiver android:name=".ReminderReceiver" android:exported="false" />

<receiver android:name=".BootReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

## Step 2: Create the Notification Helper
Creating notifications requires a channel on Android 8.0+. Create a utility class to handle channel creation and notification display.

```kotlin
// NotificationHelper.kt
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "event_reminders"
        private const val CHANNEL_NAME = "Event Reminders"
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming events"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(eventId: Int, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EVENT_ID", eventId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(eventId, notification)
    }
}
```

## Step 3: Create the Alarm Scheduler
This utility class interacts with AlarmManager to schedule and cancel exact alarms.

```kotlin
// AlarmScheduler.kt
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleExactAlarm(eventId: Int, triggerTimeMillis: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("EVENT_ID", eventId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Android 12+ requires checking if exact alarms are permitted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Fallback or request user to grant permission in system settings
                // For this lab, we use inexact as a fallback
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                return
            }
        }

        // Set exact alarm allowing execution while the device is in Doze mode
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }

    fun cancelAlarm(eventId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}
```

## Step 4: Implement the Reminder Receiver
This BroadcastReceiver is triggered when the alarm fires. It extracts the event data and triggers the notification.

```kotlin
// ReminderReceiver.kt
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getIntExtra("EVENT_ID", -1)
        
        if (eventId != -1) {
            // In a real app, you would query the Room database here 
            // using eventId to get the latest title and description.
            // For simplicity in this lab, we use generic text.
            val title = "Event Reminder"
            val message = "Your scheduled event is starting now."

            val notificationHelper = NotificationHelper(context)
            notificationHelper.createNotificationChannel()
            notificationHelper.showNotification(eventId, title, message)
        }
    }
}
```

## Step 5: Handle Device Reboots
Alarms are cleared when a device restarts. You must implement a receiver to reschedule them.

```kotlin
// BootReceiver.kt
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all pending alarms.
            // This requires querying your Room database for all future events.
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // val database = AppDatabase.getDatabase(context)
                    // val events = database.eventDao().getFutureEvents(System.currentTimeMillis())
                    // val scheduler = AlarmScheduler(context)
                    // for (event in events) {
                    //     scheduler.scheduleExactAlarm(event.id, event.timestamp)
                    // }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
```

## Step 6: Integration in ViewModel or Activity
When a user creates an event in the UI, you must save it to the database and schedule the alarm. You must also request the POST_NOTIFICATIONS permission on Android 13+.

Here is how you would coordinate the logic inside a ViewModel:

```kotlin
// EventViewModel.kt
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class EventViewModel(application: Application) : AndroidViewModel(application) {

    private val alarmScheduler = AlarmScheduler(application)
    private val notificationHelper = NotificationHelper(application)

    init {
        notificationHelper.createNotificationChannel()
    }

    fun createEvent(title: String, description: String, timeInMillis: Long) {
        viewModelScope.launch {
            // 1. Save to Room Database (Assuming an Event entity and DAO exist)
            // val eventId = eventDao.insert(Event(title = title, description = description, timestamp = timeInMillis))

            // 2. Schedule the alarm using the generated ID
            // For demonstration, using a hardcoded ID
            val eventId = 1001 
            
            if (timeInMillis > System.currentTimeMillis()) {
                alarmScheduler.scheduleExactAlarm(eventId, timeInMillis)
            }
        }
    }

    fun deleteEvent(eventId: Int) {
        viewModelScope.launch {
            // 1. Delete from Room Database
            // eventDao.delete(eventId)

            // 2. Cancel the existing alarm
            alarmScheduler.cancelAlarm(eventId)
        }
    }
}
```

## Step 7: Requesting Runtime Permissions
For Android 13 (API 33) and above, you must request the POST_NOTIFICATIONS permission before showing notifications. For Android 12 (API 31) and above, if you need exact alarms and the user has restricted them, you must guide them to system settings.

Add this logic in your MainActivity before creating an event:

```kotlin
// MainActivity.kt
import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Handle permission denial
        }
    }

    private fun checkAndRequestPermissions() {
        // Check Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Check Exact Alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Guide user to settings to allow exact alarms
                val intent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }
}
```

### Summary of key concepts:
1. AlarmManager is required for exact time reminders. WorkManager is not appropriate for this use case.
2. PendingIntent bridges the AlarmManager and your BroadcastReceiver.
3. Notifications require channels on Android 8.0+ and explicit runtime permissions on Android 13+.
4. Exact alarms require explicit user consent on Android 12+ via system settings.
5. Alarms are wiped on device reboot. You must listen for BOOT_COMPLETED and reschedule them from your database.
