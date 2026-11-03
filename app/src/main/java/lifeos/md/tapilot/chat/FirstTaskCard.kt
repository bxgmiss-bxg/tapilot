package lifeos.md.tapilot.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun FirstTaskCard(controller: ChatController, busy: Boolean) {
    val state = controller.state
    fun text(zh: String, en: String) = if (state.chinese) zh else en
    val canTry = state.ready && state.configured && !state.needsAccessibility && !busy &&
        controller.draft.isBlank() && state.attachment == null
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text("先让手机帮你算一道题", "Try a calculation on your phone"), style = MaterialTheme.typography.titleMedium)
            Text(text("用计算器算 12 × 8，看看它是否读到 96。", "Calculate 12 × 8 in Calculator and check whether Tapilot reads 96."), fontSize = 13.sp)
            Text(if (state.configured) text("✓ 模型已配置，运行时将检查实际连接", "✓ Model configured; the task will exercise the connection")
                else text("待完成：连接模型", "Next: connect a model"), fontSize = 12.sp)
            Text(if (!state.needsAccessibility) text("✓ 设备控制已连接，运行时将检查截图和操作", "✓ Device control connected; the task will check capture and actions")
                else text("待完成：开启设备控制", "Next: enable device control"), fontSize = 12.sp)
            Text(text("填入后点击发送。执行时请暂时不要操作手机，结束后核对画面，也可查看执行记录。",
                "Fill in the task, then send it. Leave the phone to Tapilot while it runs. Check the screen afterward and review the execution record."), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!state.configured) TextButton(onClick = { controller.command("settings") }, enabled = !busy) {
                    Text(text("连接模型", "Connect model"))
                } else if (state.needsAccessibility) TextButton(onClick = { controller.command("accessibility") }, enabled = !busy) {
                    Text(text("开启设备控制", "Enable control"))
                } else Button(onClick = { controller.command("firstTaskExample") }, enabled = canTry) {
                    Text(text("填入试用任务", "Fill in example"))
                }
                TextButton(onClick = { controller.command("dismissFirstTask") }, enabled = !busy) {
                    Text(text("跳过引导", "Skip guide"))
                }
            }
        }
    }
}
