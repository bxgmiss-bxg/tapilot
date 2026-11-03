package lifeos.md.tapilot.experiment

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import android.widget.*
import lifeos.md.tapilot.AppLanguage

/** Optional developer lab, never started by an agent or enabled across app restarts. */
class BackgroundDisplayActivity : Activity() {
    private lateinit var probe: BackgroundDisplayProbe
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private lateinit var input: EditText
    private var floating: FloatingDisplayWindow? = null
    private var zh = false
    private fun text(chinese: String, english: String) = if (zh) chinese else english

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        zh = AppLanguage.resolved(this) == "zh"
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(24, 40, 24, 32); setBackgroundColor(Color.WHITE)
        }
        fun label(value: String) { layout.addView(TextView(this).apply { text = value; setTextColor(Color.BLACK); textSize = 16f }) }
        label(text("后台屏实验 · 默认关闭", "Secondary display experiment · off by default"))
        label(text("仅验证系统能力，尚不支持后台运行 Agent。每次最多两分钟；关闭会释放实验屏。应用可能拒绝启动或影响主屏，请仅选择测试应用。",
            "System capability probe, not a background agent. Runs for at most two minutes. Stop releases the display. Apps may reject launch or affect the main screen; select test apps only."))
        input = EditText(this).apply {
            hint = text("在主屏输入文字，观察焦点是否受影响", "Type here to check primary-screen focus")
            setTextColor(Color.BLACK); setHintTextColor(Color.DKGRAY)
        }
        layout.addView(input)
        status = TextView(this).apply { setTextColor(Color.BLACK); textSize = 13f }
        preview = ImageView(this).apply { adjustViewBounds = true }
        probe = BackgroundDisplayProbe(this, ::render)
        fun button(chinese: String, english: String, action: () -> Unit) {
            layout.addView(Button(this).apply {
                text = text(chinese, english)
                setOnClickListener { runCatching(action).onFailure { failure ->
                    AlertDialog.Builder(this@BackgroundDisplayActivity).setMessage(failure.message)
                        .setPositiveButton(android.R.string.ok, null).show()
                } }
            })
        }
        button("开始实验", "Start experiment") { probe.start() }
        button("点击副屏测试按钮", "Tap secondary fixture") { probe.tapFixture() }
        button("选择测试应用", "Choose a test app") { chooseApp() }
        button("悬浮显示并操作", "Float and control display") {
            check(probe.active) { "Start the experiment first" }
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                if (floating == null) floating = FloatingDisplayWindow(this, probe, zh) { probe.close() }
                floating?.show()
            }
        }
        button("更新预览", "Refresh preview") { probe.refresh() }
        button("停止并释放", "Stop and release") { probe.close() }
        layout.addView(status)
        layout.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400))
        setContentView(ScrollView(this).apply { addView(layout) })
        render()
    }

    private fun chooseApp() {
        check(probe.active) { "Start the experiment first" }
        val entries = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != packageName }.sortedBy { it.loadLabel(packageManager).toString() }
        AlertDialog.Builder(this).setTitle(text("仅选择用于实验的应用", "Select a test app"))
            .setItems(entries.map { it.loadLabel(packageManager).toString() }.toTypedArray()) { _, index ->
                val selected = entries[index].activityInfo
                runCatching { probe.launch(ComponentName(selected.packageName, selected.name)) }.onFailure {
                    AlertDialog.Builder(this).setMessage(it.message).setPositiveButton(android.R.string.ok, null).show()
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun render() {
        if (!::status.isInitialized || !::probe.isInitialized) return
        status.text = "${probe.status}\nframes=${probe.frames}, taps=${probe.taps}, primaryInputFocused=${input.hasFocus()}"
        preview.setImageBitmap(probe.bitmap)
        floating?.render()
    }

    override fun onDestroy() { floating?.close(); if (::probe.isInitialized) probe.close(); super.onDestroy() }
}
