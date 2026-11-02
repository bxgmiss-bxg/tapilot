package lifeos.md.tapilot.performance

import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import lifeos.md.tapilot.MainActivity
import lifeos.md.tapilot.chat.ExecutionStep
import lifeos.md.tapilot.chat.ExecutionStepsView
import lifeos.md.tapilot.theme.TapilotTheme
import java.io.File

/** Renders the production component with explicit fixture data; no model or phone actions. */
class ExecutionStepsHarness : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        var code = 1
        val result = Bundle()
        try {
            val activity = startActivitySync(Intent(targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val output = File(targetContext.getExternalFilesDir(null), "step-progress-ui").apply { mkdirs() }
            for ((name, dark, zh) in listOf(Triple("zh-light", false, true), Triple("en-dark", true, false))) {
                runOnMainSync {
                    activity.setContentView(ComposeView(activity).apply {
                        setContent {
                            TapilotTheme(dark, "minimal") {
                                Surface(Modifier.fillMaxSize()) {
                                    Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp)) {
                                        Text(if (zh) "实际执行步骤" else "Execution steps", style = MaterialTheme.typography.titleLarge)
                                        Spacer(Modifier.height(16.dp))
                                        ExecutionStepsView(listOf(
                                            ExecutionStep("1", 1, "Launch", "Calculator", "finished"),
                                            ExecutionStep("2", 2, "Plan", "", "finished"),
                                            ExecutionStep("3", 3, "Tap", if (zh) "数字 1" else "Digit 1", "finished"),
                                            ExecutionStep("4", 4, "Tap", if (zh) "数字 2" else "Digit 2", "finished"),
                                            ExecutionStep("5", 5, "Tap", if (zh) "乘号" else "Multiply button", "failed"),
                                            ExecutionStep("6", 6, "Query", "", "interrupted"),
                                        ), zh)
                                        Spacer(Modifier.height(16.dp))
                                        Text(if (zh) "界面验证数据；不是实际任务执行结果。" else "UI fixture data; not a real task result.")
                                    }
                                }
                            }
                        }
                    })
                }
                waitForIdleSync()
                SystemClock.sleep(700)
                val bitmap = checkNotNull(getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).takeScreenshot())
                File(output, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
            runOnMainSync { activity.finish() }
            result.putString("evidence", output.absolutePath)
            code = 0
        } catch (error: Throwable) { result.putString("failure", error.toString()) }
        finish(code, result)
    }
}
