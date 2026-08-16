# Lab 8: Create a Functional Media Player or Camera Application

Complete step-by-step guide.

## Lab Overview
This lab offers two practical paths: building a **Camera Application** using CameraX (the recommended Jetpack library) or a **Media Player** using Media3 ExoPlayer. Choose one based on your learning goal.

## Path Comparison

| Feature | Camera Application (CameraX) | Media Player (ExoPlayer) |
|---------|------------------------------|--------------------------|
| **Primary Use** | Capture photos/videos | Play audio/video files |
| **Key Library** | `androidx.camera` | `androidx.media3` |
| **Abstraction Level** | High-level, easy-to-use APIs | High-level, built on ExoPlayer |
| **Learning Focus** | Camera hardware, permissions, use cases | Media playback, lifecycle, controls |
| **Complexity** | Medium (permissions, async operations) | Medium (lifecycle, state management) |

---

## Path 1: Camera Application with CameraX

CameraX is a Jetpack library that provides a consistent and easy-to-use API for camera operations, working across most Android devices.

### Step 1: Add CameraX Dependencies

In your app-level `build.gradle`, add the CameraX dependencies:

```gradle
dependencies {
    // CameraX core library using camera2 implementation
    def camerax_version = "1.3.0-alpha02"
    implementation "androidx.camera:camera-core:${camerax_version}"
    implementation "androidx.camera:camera-camera2:${camerax_version}"
    implementation "androidx.camera:camera-lifecycle:${camerax_version}"
    implementation "androidx.camera:camera-video:${camerax_version}"
    implementation "androidx.camera:camera-view:${camerax_version}"
    implementation "androidx.camera:camera-extensions:${camerax_version}"
}
```

### Step 2: Declare Permissions

In `AndroidManifest.xml`, add the necessary permissions:

```xml
<uses-feature android:name="android.hardware.camera.any" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />
```

### Step 3: Request Runtime Permissions

Create a permission request mechanism. For production apps, you should explain to users why you need these permissions.

```kotlin
class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", 
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}
```

### Step 4: Implement Camera Preview

Use `ProcessCameraProvider` to bind the camera to your lifecycle.

```kotlin
private var imageCapture: ImageCapture? = null
private var videoCapture: VideoCapture<Recorder>? = null
private lateinit var cameraExecutor: ExecutorService

private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener({
        // Used to bind the lifecycle of cameras to the lifecycle owner
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        // Preview
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        // Select back camera as a default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // Image Capture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        // Video Capture
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, videoCapture
            )

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }

    }, ContextCompat.getMainExecutor(this))

    cameraExecutor = Executors.newSingleThreadExecutor()
}
```

### Step 5: Capture Photos

Implement photo capture and save to external storage.

```kotlin
private fun takePhoto() {
    // Get a stable reference of the modifiable image capture use case
    val imageCapture = imageCapture ?: return

    // Create time stamped name and MediaStore entry.
    val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
        .format(System.currentTimeMillis())

    // Create output options object which contains file + metadata
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        .build()

    // Set up image capture listener which is triggered after photo has
    // been taken
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val msg = "Photo capture succeeded: ${output.savedUri}"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, msg)
            }
        }
    )
}
```

### Step 6: Capture Video

Implement video capture with recording state management.

```kotlin
private var recording: Recording? = null

private fun captureVideo() {
    val videoCapture = this.videoCapture ?: return

    if (recording != null) {
        // Stop the current recording
        recording?.stop()
        recording = null
        return
    }

    // Create time stamped name and MediaStore entry.
    val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
        .format(System.currentTimeMillis())

    // Create output options object which contains file + metadata
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
        }
    }

    val mediaStoreOutput = MediaStoreOutputOptions
        .Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
        .setContentValues(contentValues)
        .build()

    // Start recording
    recording = videoCapture.output
        .prepareRecording(this, mediaStoreOutput)
        .apply {
            if (PermissionChecker.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.RECORD_AUDIO
                ) == PermissionChecker.PERMISSION_GRANTED
            ) {
                withAudioEnabled()
            }
        }
        .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
            when (recordEvent) {
                is RecordEvent.Start -> {
                    binding.videoCaptureButton.text = "STOP"
                }
                is RecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        val msg = "Video capture succeeded: ${recordEvent.outputUri}"
                        Toast.makeText(
                            baseContext, msg, Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, msg)
                    } else {
                        recording?.close()
                        recording = null
                        Log.e(
                            TAG, "Video capture ends with error: " +
                                    recordEvent.error
                        )
                    }
                    binding.videoCaptureButton.text = "START"
                }
            }
        }
}
```

### Step 7: Layout and UI

Create a simple layout with a `PreviewView` and capture buttons:

```xml
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.camera.view.PreviewView
        android:id="@+id/viewFinder"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <Button
        android:id="@+id/image_capture_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="50dp"
        android:text="Take Photo"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toStartOf="@+id/video_capture_button" />

    <Button
        android:id="@+id/video_capture_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="50dp"
        android:text="Start Video"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toEndOf="@+id/image_capture_button"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## Path 2: Media Player with Media3 ExoPlayer

Media3 ExoPlayer is the modern standard for media playback on Android, providing robust and customizable playback.

### Step 1: Add Media3 Dependencies

In your app-level `build.gradle`:

```gradle
dependencies {
    // Media3 ExoPlayer dependencies
    implementation "androidx.media3:media3-exoplayer:1.3.0"
    implementation "androidx.media3:media3-ui:1.3.0"
    implementation "androidx.media3:media3-common:1.3.0"
}
```

### Step 2: Add PlayerView to Layout

Add the `PlayerView` to your XML layout:

```xml
<com.google.android.exoplayer2.ui.PlayerView
    android:id="@+id/playerView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:resize_mode="fit"
    app:show_buffering="when_playing"
    app:use_controller="true" />
```

### Step 3: Initialize ExoPlayer

Create and configure the player instance:

```kotlin
class MediaPlayerActivity : AppCompatActivity() {
    
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)
    }
    
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                // Bind the player to the view.
                binding.playerView.player = exoPlayer
                
                // Create a media item.
                val mediaItem = MediaItem.fromUri(
                    "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
                )
                
                // Set the media item to be played.
                exoPlayer.setMediaItem(mediaItem)
                
                // Prepare the player.
                exoPlayer.prepare()
                
                // Set play when ready and seek to previous position if available.
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(currentItem, playbackPosition)
            }
    }
    
    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
    }
}
```

### Step 4: Manage Player Lifecycle

Properly manage the player lifecycle to avoid memory leaks and playback issues:

```kotlin
override fun onStart() {
    super.onStart()
    if (Util.SDK_INT > 23) {
        initializePlayer()
    }
}

override fun onResume() {
    super.onResume()
    if ((Util.SDK_INT <= 23 || player == null)) {
        initializePlayer()
    }
}

override fun onPause() {
    super.onPause()
    if (Util.SDK_INT <= 23) {
        releasePlayer()
    }
}

override fun onStop() {
    super.onStop()
    if (Util.SDK_INT > 23) {
        releasePlayer()
    }
}
```

### Step 5: Add Playback Controls

The `PlayerView` already includes default controls, but you can customize them:

```kotlin
// Customizing player controls
binding.playerView.apply {
    // Show controller automatically
    controllerShowTimeoutMs = 3000
    
    // Hide controller on touch
    controllerHideOnTouch = true
    
    // Set custom play/pause actions
    setPlayWhenReady(player?.playWhenReady ?: false)
}
```

### Step 6: Handle Different Media Sources

ExoPlayer supports various media sources:

```kotlin
// Progressive media source (MP4, etc.)
val progressiveSource = ProgressiveMediaSource.Factory(
    DefaultDataSource.Factory(this)
).createMediaSource(MediaItem.fromUri(videoUrl))

// DASH media source
val dashSource = DashMediaSource.Factory(
    DefaultDataSource.Factory(this)
).createMediaSource(MediaItem.fromUri(dashUrl))

// HLS media source
val hlsSource = HlsMediaSource.Factory(
    DefaultDataSource.Factory(this)
).createMediaSource(MediaItem.fromUri(hlsUrl))

// Set multiple media items to create a playlist
val mediaItems = listOf(
    MediaItem.fromUri("https://example.com/video1.mp4"),
    MediaItem.fromUri("https://example.com/video2.mp4"),
    MediaItem.fromUri("https://example.com/video3.mp4")
)
player?.setMediaItems(mediaItems)
player?.prepare()
```

### Step 7: Implement Advanced Features

<summary> Advanced Player Features</summary>

```kotlin
// 1. Background playback
// Create a foreground service for background playback
class AudioPlayerService : Service() {
    private var player: ExoPlayer? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Initialize player with audio source
        initializePlayer()
        return START_STICKY
    }
}

// 2. Media session for integration with other apps
val mediaSession = MediaSession.Builder(this, player!!)
    .setCallback(MyMediaSessionCallback())
    .build()

// 3. Custom error handling
player?.addListener(object : Player.Listener {
    override fun onPlayerError(error: PlaybackException) {
        when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                // Handle network error
            }
            PlaybackException.ERROR_CODE_DECODING_FAILED -> {
                // Handle decoding error
            }
            // Handle other errors
        }
    }
})
```

---

## Testing Your Implementation

### Camera App Testing:
- [ ] App requests camera and audio permissions
- [ ] Camera preview displays correctly
- [ ] Photo capture saves to device gallery
- [ ] Video capture saves to device gallery
- [ ] Captured media is playable
- [ ] App handles camera errors gracefully

### Media Player Testing:
- [ ] Player initializes and displays content
- [ ] Playback controls work (play, pause, seek)
- [ ] Player handles different media formats
- [ ] Player state is preserved across configuration changes
- [ ] Player releases resources properly
- [ ] Background playback works (if implemented)

## Best Practices & Considerations

### Camera App Best Practices:
1. **Always request permissions** before using camera features 【turn0search3】
2. **Handle device compatibility** - not all devices support all camera features
3. **Use appropriate capture mode** - minimize latency for photos, maximize quality for videos
4. **Manage camera executor** properly to avoid memory leaks
5. **Test on multiple devices** - camera hardware varies significantly

### Media Player Best Practices:
1. **Always release the player** in `onStop()` or `onDestroy()`
2. **Preserve playback state** across configuration changes
3. **Use appropriate buffer sizes** for your content type
4. **Handle network errors** gracefully with user feedback
5. **Consider battery optimization** for long playback sessions

## Additional Resources

- **CameraX Documentation**: [Getting Started with CameraX](https://developer.android.com/codelabs/camerax-getting-started) 
- **CameraX Samples**: [Android Camera Samples Catalog](https://github.com/android/camera-samples) 
- **Media3 Documentation**: [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer) 
- **ExoPlayer Guide**: [ExoPlayer in Android 2022 — Getting Started](https://medium.com/codex/exoplayer-in-android-2022-getting-started-6edcb2b399e5)
