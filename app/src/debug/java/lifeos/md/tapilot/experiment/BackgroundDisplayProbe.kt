package lifeos.md.tapilot.experiment

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.Presentation
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import lifeos.md.tapilot.TaskExecutionTracker
import lifeos.md.tapilot.accessibility.MidsceneAccessibilityService
import lifeos.md.tapilot.wireless.DeviceControlLease

/** Debug-only experiment. Never routes production device commands or falls back to display 0. */
internal class BackgroundDisplayProbe(private val host: Activity, private val changed: () -> Unit) : AutoCloseable {
    private val handler = Handler(Looper.getMainLooper())
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var presentation: Presentation? = null
    private var target: Button? = null
    private var leased = false
    private var generation = 0
    private val timeout = Runnable { close(); status = "Experiment timed out; display released"; changed() }
    var bitmap: Bitmap? = null; private set
    var frames = 0; private set
    var taps = 0; private set
    var status = "Off"; private set
    val displayId get() = display?.display?.displayId
    val active get() = display != null
    private var snapshotRequested = true

    fun start() {
        check(!active)
        check(!TaskExecutionTracker.isActive) { "Stop the current task before starting this experiment" }
        DeviceControlLease.acquire(OWNER)
        leased = true
        try {
            generation++
            val run = generation
            frames = 0; taps = 0; snapshotRequested = true
            val images = ImageReader.newInstance(WIDTH, HEIGHT, PixelFormat.RGBA_8888, 2)
            reader = images
            images.setOnImageAvailableListener({ source ->
                if (run != generation) return@setOnImageAvailableListener
                source.acquireLatestImage()?.use { image ->
                    frames++
                    if (snapshotRequested) {
                        val plane = image.planes[0]
                        val paddedWidth = plane.rowStride / plane.pixelStride
                        val padded = Bitmap.createBitmap(paddedWidth, HEIGHT, Bitmap.Config.ARGB_8888)
                        padded.copyPixelsFromBuffer(plane.buffer)
                        val next = Bitmap.createBitmap(padded, 0, 0, WIDTH, HEIGHT)
                        if (next !== padded) padded.recycle()
                        // The activity replaces its ImageView reference before the old bitmap is recycled.
                        val old = bitmap
                        bitmap = next; snapshotRequested = false
                        changed()
                        old?.recycle()
                    }
                }
            }, handler)
            display = host.getSystemService(DisplayManager::class.java).createVirtualDisplay(
                "Tapilot optional display probe", WIDTH, HEIGHT, 160, images.surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
            ) ?: error("Virtual display creation returned no display")
            check(displayId != Display.DEFAULT_DISPLAY) { "Refusing to target the primary display" }
            showFixture()
            status = "Display $displayId created; waiting for rendered pixels"
            handler.postDelayed(timeout, 120_000)
            changed()
        } catch (failure: Exception) { close(); throw failure }
    }

    private fun showFixture() {
        val panel = Presentation(host, checkNotNull(display).display)
        presentation = panel
        val layout = LinearLayout(panel.context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(224, 245, 255))
        }
        layout.addView(TextView(panel.context).apply {
            text = "Tapilot secondary display\nDisplay $displayId"; textSize = 22f; gravity = Gravity.CENTER
        })
        target = Button(panel.context).apply {
            text = "Fixture taps: 0"
            setOnClickListener { taps++; text = "Fixture taps: $taps"; snapshotRequested = true; changed() }
        }
        layout.addView(target, LinearLayout.LayoutParams(360, 100))
        panel.setContentView(layout)
        panel.show()
    }

    fun tapFixture() {
        val id = checkNotNull(displayId) { "Start the experiment first" }
        check(id != Display.DEFAULT_DISPLAY)
        val button = checkNotNull(target) { "Test tap is only available on our fixture" }
        val service = checkNotNull(MidsceneAccessibilityService.current()) { "Enable this test app's device control first" }
        val point = IntArray(2); button.getLocationOnScreen(point)
        val path = Path().apply { moveTo(point[0] + button.width / 2f, point[1] + button.height / 2f) }
        val gesture = GestureDescription.Builder().setDisplayId(id)
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80)).build()
        val run = generation
        val accepted = service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                if (run != generation) return
                status = "Gesture dispatched to display $id; fixture taps observed: $taps"
                changed()
            }
            override fun onCancelled(gestureDescription: GestureDescription) {
                if (run != generation) return
                status = "Secondary-display gesture cancelled"; changed()
            }
        }, handler)
        check(accepted) { "System refused the secondary-display gesture" }
        status = "Waiting for gesture result"; changed()
    }

    fun launch(component: ComponentName) {
        val id = checkNotNull(displayId) { "Start the experiment first" }
        check(id != Display.DEFAULT_DISPLAY)
        check(host.packageManager.hasSystemFeature(PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS)) {
            "Device does not advertise secondary-display activities; launch refused"
        }
        val intent = Intent.makeMainActivity(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        check(host.getSystemService(ActivityManager::class.java).isActivityStartAllowedOnDisplay(host, id, intent)) { "System disallows this app on the experiment display" }
        host.startActivity(intent, ActivityOptions.makeBasic().setLaunchDisplayId(id).toBundle())
        presentation?.dismiss(); presentation = null; target = null
        snapshotRequested = true
        status = "Launch requested on display $id. Inspect preview and main screen; success is not established."
        changed()
    }

    fun refresh() {
        check(active)
        snapshotRequested = true
        // A static surface may not produce another frame; retain the last actual frame.
        target?.invalidate()
        status = "Latest captured frame shown; waiting for a new frame"; changed()
    }

    override fun close() {
        generation++
        handler.removeCallbacks(timeout)
        presentation?.dismiss(); presentation = null; target = null
        display?.release(); display = null
        reader?.setOnImageAvailableListener(null, null)
        reader?.close(); reader = null
        val old = bitmap; bitmap = null
        if (leased) DeviceControlLease.release(OWNER)
        leased = false
        status = "Off; experiment resources released"
        changed()
        old?.recycle()
    }

    companion object {
        private const val OWNER = "background-display-probe"
        private const val WIDTH = 480
        private const val HEIGHT = 800
    }
}
