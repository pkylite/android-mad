# Lab 4 - An App Widget updated periodically by a background worker

Complete step-by-step guide.

## Introduction
In modern Android development, traditional background `Service` classes are heavily restricted by Doze mode and App Standby buckets, and will crash on Android 8+ if started from the background. 

To implement a **background task that runs periodically**, the officially supported and recommended approach is to use **WorkManager**. WorkManager guarantees execution even if the app is closed or the device restarts, and it defers gracefully to respect battery life.

### Step 1: Add WorkManager Dependency
Open your `build.gradle` (Module: app) and add the WorkManager dependency to your `dependencies` block:

```gradle
dependencies {
    // ... other dependencies
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```
*Sync your project after adding this.*

### Step 2: Create the Widget Layout
Create a simple layout for your home screen widget. 

Right-click `res/layout` -> New -> Layout Resource File. Name it `widget_layout.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#2196F3"
    android:padding="12dp">

    <TextView
        android:id="@+id/tvWidgetText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_centerInParent="true"
        android:text="Waiting for update..."
        android:textColor="#FFFFFF"
        android:textSize="18sp"
        android:textStyle="bold" />

</RelativeLayout>
```

### Step 3: Define Widget Properties
Create an XML file to define the widget's size and update interval.

Right-click `res/xml` (create the `xml` folder if it doesn't exist) -> New -> XML Resource File. Name it `widget_info.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:initialLayout="@layout/widget_layout"
    android:minWidth="250dp"
    android:minHeight="100dp"
    android:updatePeriodMillis="0"
    android:widgetCategory="home_screen">
    <!-- 
    Note: We set updatePeriodMillis to 0 because we are handling the 
    periodic update manually via WorkManager. If we set it to > 0, 
    Android forces a minimum of 30 minutes anyway.
    -->
</appwidget-provider>
```

### Step 4: Implement the AppWidgetProvider
This is the class that receives broadcasts from the system (or our Worker) and updates the widget UI. Because widgets run in the Home Screen's process, we use `RemoteViews` to update the UI.

Create a new Kotlin class named `MyWidgetProvider.kt`:

```kotlin
package com.example.widgetlab4 // Replace with your package

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class MyWidgetProvider : AppWidgetProvider() {

    companion object {
        // Helper function called by our background Worker to push updates
        fun updateWidget(context: Context, text: String) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            
            views.setTextViewText(R.id.tvWidgetText, text)

            // Get all widget IDs and update them
            val widgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, MyWidgetProvider::class.java)
            )
            for (widgetId in widgetIds) {
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Called when the widget is first placed on the home screen
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        views.setTextViewText(R.id.tvWidgetText, "Widget Added!")

        for (widgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Start our periodic background task when the first widget is placed
        WidgetUpdateWorker.schedulePeriodicUpdates(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Stop periodic updates when the last widget is removed
        WidgetUpdateWorker.cancelPeriodicUpdates(context)
    }
}
```

### Step 5: Implement the Background Worker (The "Service")
This is the modern equivalent of a background service. It will run in the background, fetch or generate data, and tell the widget to update.

Create a new Kotlin class named `WidgetUpdateWorker.kt`:

```kotlin
package com.example.widgetlab4 // Replace with your package

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Do background work (e.g., fetch from API, get current time)
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val updateText = "Updated at: $currentTime"

            // 2. Tell the Widget Provider to update the UI
            MyWidgetProvider.updateWidget(applicationContext, updateText)

            // 3. Indicate success
            Result.success()
        } catch (e: Exception) {
            // Indicate failure so WorkManager can retry based on its BackoffPolicy
            Result.failure()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "widget_periodic_update"

        fun schedulePeriodicUpdates(context: Context) {
            // Android enforces a minimum interval of 15 minutes for periodic work
            val periodicWorkRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15, TimeUnit.MINUTES // Repeat every 15 minutes
            )
                .setInitialDelay(5, TimeUnit.SECONDS) // Optional: small delay before first run
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Don't replace if already scheduled
                periodicWorkRequest
            )
        }

        fun cancelPeriodicUpdates(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
```

### Step 6: Register the Widget in the Manifest
Unlike Activities, `AppWidgetProvider` is a `BroadcastReceiver`, so it must be registered in the `AndroidManifest.xml` inside the `<application>` tag:

```xml
<receiver android:name=".MyWidgetProvider"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/widget_info" />
</receiver>
```

---

### How to Test the Lab
1. **Run the app** on a device or emulator.
2. Go to the Home Screen, long-press an empty area, and select **Widgets**.
3. Find your app's widget in the list and drag it to the home screen.
4. You will immediately see "Widget Added!".
5. **Wait 15 minutes** (or change the `TimeUnit.MINUTES` to the minimum allowed limit during debugging). 
6. When the `WorkManager` job triggers in the background, the text will change to "Updated at: [Current Time]".

### 💡 Lab Note: Why WorkManager instead of a Service?
The concept of `Services` is an outdated (pre-Android 8.0) pattern. Historically, this was done using an `IntentService` triggered by an `AlarmManager`. However, Google explicitly states: 
> *"WorkManager is the recommended solution for persistent work... It replaces the need for JobScheduler, BroadcastReceiver, and Services running in the background."*

The use of `Service` for academic compliance, would implement the legacy `AlarmManager + IntentService` code, which **will crash** on modern Android versions unless properly converted to a `ForegroundService` (which a user cannot run silently from a widget click on Android 12+). Hence, WorkManager is the only reliable way to do this in 2026.