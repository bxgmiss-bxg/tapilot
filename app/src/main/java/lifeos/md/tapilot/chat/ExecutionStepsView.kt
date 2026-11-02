package lifeos.md.tapilot.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ExecutionStepsView(steps: List<ExecutionStep>, zh: Boolean) {
    if (steps.isEmpty()) return
    Text(if (zh) "实际执行步骤" else "Execution steps", fontSize = 12.sp)
    Text(if (zh) "“已执行”表示该步骤结束，任务结果仍需核验。输入内容不会显示在这里。"
        else "Executed means the step ended; the task result still needs verification. Input values are omitted.",
        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (steps.first().number > 1) Text(if (zh) "仅显示最近 ${steps.size} 步；完整记录见执行报告。"
        else "Showing the latest ${steps.size} steps. See the report for the full record.", fontSize = 11.sp)
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
        items(steps, key = { it.id }) { step ->
            SelectionContainer {
                Text("${step.number}. ${step.label(zh)} · ${step.stateLabel(zh)}",
                    fontSize = 12.sp, color = if (step.status == "failed") MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
