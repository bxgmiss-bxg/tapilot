package lifeos.md.tapilot.performance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import lifeos.md.tapilot.TapilotApplication
import org.json.JSONObject
import java.io.File

/** Shell-only setup for the disposable CI package; never submits or completes tasks. */
class CiModelSetupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.packageName != "lifeos.md.tapilot.e2everify") {
            setResult(1, "CI setup requires the isolated verification package", null)
            return
        }
        val file = File(context.filesDir, "ci-model-key")
        try {
            val runtime = (context.applicationContext as TapilotApplication).assistant
            check(runtime.coordinator.ready && !runtime.state.running)
            val key = file.readText().trim()
            check(key.isNotBlank())
            runtime.coordinator.selectPreset("openai")
            runtime.coordinator.saveModel(JSONObject()
                .put("baseUrl", "https://api.openai.com/v1")
                .put("modelName", "gpt-6.1-terra").put("modelFamily", "gpt-6"), key)
            runtime.coordinator.preference("chatgptConversation", false)
            check(runtime.state.configured)
            setResult(0, "CI model configured", null)
        } catch (_: Exception) {
            // The setup input and provider errors must never reach CI artifacts.
            setResult(1, "CI model setup failed", null)
        } finally {
            file.delete()
        }
    }
}
