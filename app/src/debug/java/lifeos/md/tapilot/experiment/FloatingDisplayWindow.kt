package lifeos.md.tapilot.experiment

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** A real captured display preview. All input is explicitly addressed to the secondary display. */
@SuppressLint("ClickableViewAccessibility")
internal class FloatingDisplayWindow(
    private val host: Activity,
    private val probe: BackgroundDisplayProbe,
    private val zh: Boolean,
    private val stop: () -> Unit,
) : AutoCloseable {
    private val manager = host.getSystemService(WindowManager::class.java)
    private var root: LinearLayout? = null
    private var preview: ImageView? = null
    private var feedback: TextView? = null
    private var path: Path? = null
    private var downAt = 0L
    private var samples = 0
    private fun text(chinese: String, english: String) = if (zh) chinese else english
    private fun dp(value: Int) = (value * host.resources.displayMetrics.density).toInt()

    fun show() {
        check(probe.active) { "Start the experiment first" }
        check(Settings.canDrawOverlays(host)) { "Allow display over other apps first" }
        if (root != null) return
        val bounds = manager.currentWindowMetrics.bounds
        val width = minOf(dp(240), bounds.width() * 2 / 3)
        val params = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.LEFT; x = dp(12); y = dp(80) }
        val panel = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(235, 242, 246))
            elevation = dp(8).toFloat()
        }
        val header = LinearLayout(host)
        val handle = TextView(host).apply {
            text = text("虚拟屏 · 拖动", "Virtual screen · drag")
            setTextColor(Color.BLACK); textSize = 13f; gravity = Gravity.CENTER
        }
        header.addView(handle, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(Button(host).apply {
            text = "×"
            contentDescription = text("关闭并释放虚拟屏", "Close and release virtual display")
            setOnClickListener { stop() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        panel.addView(header)
        var initialX = 0; var initialY = 0; var downX = 0f; var downY = 0f
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { initialX = params.x; initialY = params.y; downX = event.rawX; downY = event.rawY }
                MotionEvent.ACTION_MOVE -> {
                    val current = manager.currentWindowMetrics.bounds
                    params.x = (initialX + event.rawX - downX).toInt().coerceIn(0, (current.width() - panel.width).coerceAtLeast(0))
                    params.y = (initialY + event.rawY - downY).toInt().coerceIn(0, (current.height() - panel.height).coerceAtLeast(0))
                    manager.updateViewLayout(panel, params)
                }
            }
            true
        }
        val image = ImageView(host).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
            contentDescription = text("虚拟屏实时画面，可点击或滑动", "Live virtual display; tap or swipe")
            setOnTouchListener { view, event -> onPreviewTouch(view, event) }
        }
        // Keep the display's aspect ratio, including on small phones.
        val previewHeight = minOf(width * BackgroundDisplayProbe.HEIGHT / BackgroundDisplayProbe.WIDTH, bounds.height() / 2)
        panel.addView(image, LinearLayout.LayoutParams(width, previewHeight))
        val label = TextView(host).apply { setTextColor(Color.BLACK); textSize = 11f; setPadding(dp(6), dp(4), dp(6), dp(4)); maxLines = 4 }
        panel.addView(label)
        manager.addView(panel, params)
        root = panel; preview = image; feedback = label
        probe.livePreview = true
        render()
        probe.refresh()
    }

    private fun onPreviewTouch(view: View, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_CANCEL || event.pointerCount != 1 || !probe.active) {
            path = null
            return true
        }
        val point = PreviewCoordinates.map(event.x, event.y, view.width, view.height,
            BackgroundDisplayProbe.WIDTH, BackgroundDisplayProbe.HEIGHT)
        if (point == null) { path = null; return true }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                path = Path().apply { moveTo(point.first, point.second) }
                downAt = event.eventTime; samples = 1
            }
            MotionEvent.ACTION_MOVE -> {
                if (++samples > 256) path = null
                path?.lineTo(point.first, point.second)
            }
            MotionEvent.ACTION_UP -> {
                val completed = path
                path = null
                if (completed != null) {
                    completed.lineTo(point.first, point.second)
                    runCatching { probe.dispatchPath(completed, (event.eventTime - downAt).coerceAtLeast(80)) }
                        .onFailure { feedback?.text = it.message }
                    view.performClick()
                }
            }
        }
        return true
    }

    fun render() {
        if (!probe.active) { close(); return }
        preview?.setImageBitmap(probe.bitmap)
        feedback?.text = "${text("仅副屏输入；关闭会释放", "Secondary input only; close releases")}\n${probe.status}"
    }

    override fun close() {
        probe.livePreview = false
        path = null
        preview?.setImageDrawable(null)
        root?.let { manager.removeViewImmediate(it) }
        root = null; preview = null; feedback = null
    }
}
