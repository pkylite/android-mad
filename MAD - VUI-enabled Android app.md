# Lab 2 - VUI-enabled Android app

Complete step-by-step guide.

## Introduction

To build a basic VUI-enabled Android app using the native `SpeechRecognizer`, we need to handle runtime permissions, initialize the recognizer, and listen for voice input callbacks.

### Step 1: Add Permissions to Manifest

The `SpeechRecognizer` requires audio recording permissions. Open your `AndroidManifest.xml` and add the following above the `<application>` tag:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" /> <!-- Often needed for cloud-based recognition fallback -->
```

### Step 2: Create the User Interface

We need a button to start/stop listening and a TextView to display the recognized text or status. Open `res/layout/activity_main.xml` and replace the contents with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="24dp"
    android:gravity="center">

    <TextView
        android:id="@+id/tvStatus"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Tap the button and speak"
        android:textSize="18sp"
        android:layout_marginBottom="24dp" />

    <TextView
        android:id="@+id/tvResult"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text=""
        android:textSize="24sp"
        android:textStyle="bold"
        android:gravity="center"
        android:layout_marginBottom="32dp" />

    <Button
        android:id="@+id/btnSpeak"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Start Listening" />

</LinearLayout>
```

### Step 3: Write the Kotlin Code (MainActivity)

Open `MainActivity.kt`. We will use the modern `registerForActivityResult` for permissions and implement `RecognitionListener` to handle speech events.

```kotlin
package com.example.vuilab3 // Replace with your actual package name

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var btnSpeak: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView

    private var isListening = false

    // Modern permission handling
    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(this, "Audio permission is required for VUI", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSpeak = findViewById(R.id.btnSpeak)
        tvStatus = findViewById(R.id.tvStatus)
        tvResult = findViewById(R.id.tvResult)

        // Check if SpeechRecognizer is available on the device
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech Recognition not available on this device", Toast.LENGTH_LONG).show()
            btnSpeak.isEnabled = false
            return
        }

        setupSpeechRecognizer()

        btnSpeak.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                checkPermissionAndStartListening()
            }
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // You can change the locale
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Get real-time partial results
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvStatus.text = "Listening..."
                isListening = true
                btnSpeak.text = "Stop Listening"
            }

            override fun onBeginningOfSpeech() {
                tvStatus.text = "Speech started..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                // You can use this to animate a volume meter
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                tvStatus.text = "Processing..."
                isListening = false
                btnSpeak.text = "Start Listening"
            }

            override fun onError(error: Int) {
                val errorMessage = getErrorText(error)
                tvStatus.text = "Error: $errorMessage"
                isListening = false
                btnSpeak.text = "Start Listening"
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    tvResult.text = matches[0] // Display the most confident result
                }
                tvStatus.text = "Tap the button and speak"
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    tvResult.text = matches[0] // Update text in real-time as user speaks
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun checkPermissionAndStartListening() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }
        speechRecognizer.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer.stopListening()
        isListening = false
        btnSpeak.text = "Start Listening"
        tvStatus.text = "Stopped"
    }

    // Helper to translate error codes to readable text
    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Prevent memory leaks
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }
}
```

### Key Concepts Explained (For your Lab understanding):

1. **`SpeechRecognizer.createSpeechRecognizer(this)`**: Instantiates the recognizer. Always check `isRecognitionAvailable` first, as emulators often lack native Google voice services.
2. **`RecognizerIntent`**: Configures the recognizer.
   - `LANGUAGE_MODEL_FREE_FORM`: Used for general, unconstrained dictation (as opposed to `LANGUAGE_MODEL_WEB_SEARCH` which is optimized for short search queries).
3. **`RecognitionListener`**: The callback interface.
   - `onReadyForSpeech`: Fires when the mic is open and ready.
   - `onPartialResults`: Fires repeatedly as the user is speaking (live transcription).
   - `onResults`: Fires when the recognizer finalizes the text after the user stops speaking.
   - `onError`: Crucial for debugging. Error 6 (`ERROR_NO_MATCH`) or 7 (`ERROR_SPEECH_TIMEOUT`) are common if the user is silent.
4. **Permissions**: Since Android 6.0, `RECORD_AUDIO` is a "Dangerous" permission. The app will crash if you don't request it at runtime (which we handle using `registerForActivityResult`).
5. **Lifecycle Management**: Calling `speechRecognizer.destroy()` in `onDestroy()` is mandatory to release the microphone and prevent memory leaks.

### Testing the App

- **Real Device Needed:** The native `SpeechRecognizer` usually relies on the Google app/services. It **will not work on a standard Android Emulator** unless you have passed-through a microphone and installed Google Play Services. Test on a physical Android phone.
- Tap "Start Listening", say a sentence, and watch the text update in real-time via `onPartialResults`, then finalize via `onResults`.
