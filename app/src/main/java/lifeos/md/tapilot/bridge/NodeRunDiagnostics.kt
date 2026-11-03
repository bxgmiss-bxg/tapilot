package lifeos.md.tapilot.bridge

/** A missing report is a symptom; retain the worker's failure or crash diagnostic. */
internal fun nodeRunIncompleteError(
    exitCode: Int,
    failureMessage: String?,
    logs: List<String>,
    environment: Map<String, String> = emptyMap(),
): IllegalStateException {
    val diagnostic = failureMessage?.takeIf { it.isNotBlank() }
        ?: logs.lastOrNull {
            Regex("CANNOT LINK EXECUTABLE|(?:^|\\s)(?:[A-Za-z]*Error:|FATAL ERROR:)|Abort message:|bad option:")
                .containsMatchIn(it)
        }?.trim()
    var message = "Node runtime exited before returning a complete report (exit code $exitCode)" +
        diagnostic?.let { ": $it" }.orEmpty()
    // Errors may quote model configuration. Never forward configured credentials.
    environment.filterKeys { key ->
        Regex("KEY|TOKEN|SECRET|PASSWORD").containsMatchIn(key)
    }.values.filter { it.isNotBlank() }.sortedByDescending { it.length }.forEach {
        message = message.replace(it, "[REDACTED]")
    }
    return IllegalStateException(message.take(2000))
}
