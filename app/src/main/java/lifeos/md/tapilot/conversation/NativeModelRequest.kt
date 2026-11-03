package lifeos.md.tapilot.conversation

import lifeos.md.tapilot.AppText.text as appText
import lifeos.md.tapilot.chatgpt.ChatGptResponses
import java.net.URI
import java.security.MessageDigest
import java.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object NativeModelRequest {
    fun usesResponses(model: JSONObject): Boolean = URI(model.getString("baseUrl")).host == "api.openai.com" &&
        model.getString("modelName").startsWith("gpt-6")

    /** Opaque replay belongs to exactly this endpoint, model and credential. */
    fun responsesScope(model: JSONObject, key: String): String? = if (usesResponses(model)) {
        val identity = "${model.getString("baseUrl")}\u0000${model.getString("modelName")}\u0000$key"
        "openai-api:" + MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) }
    } else null

    fun params(model: JSONObject, key: String, messages: JSONArray, tools: JSONArray = JSONArray().put(NativeConversationAgent.tool)): JSONObject {
        val endpoint = ModelProfiles.endpoint(model.getString("baseUrl"))
        val scope = responsesScope(model, key)
        val chatMessages = JSONArray(messages.toString())
        for (i in 0 until chatMessages.length()) chatMessages.getJSONObject(i).apply {
            if (scope == null || optString("tapilotResponsesAccount") != scope) remove("tapilotResponsesOutput")
            remove("tapilotResponsesAccount")
        }
        if (scope != null) {
            // Reuse the tested stateless Responses codec, with API-key auth and replay isolation.
            val body = ChatGptResponses.body(model.getString("modelName"), chatMessages, tools)
            return JSONObject().put("url", endpoint.removeSuffix("/chat/completions") + "/responses").put("method", "POST")
                .put("headers", JSONObject().put("Authorization", "Bearer $key").put("Content-Type", "application/json").put("Accept", "text/event-stream"))
                .put("bodyBase64", Base64.getEncoder().encodeToString(body.toString().toByteArray()))
        }
        val body = JSONObject().put("model", model.getString("modelName")).put("messages", chatMessages)
            .put("tools", tools).put("tool_choice", "auto").put("stream", false)
        // DashScope hybrid Qwen models default to reasoning; use a fast, non-streaming reply.
        val host = URI(endpoint).host
        val modernOpenAi = host == "api.openai.com" &&
            model.getString("modelName").matches(Regex("(?:gpt-[5-9].*|o[1-9].*)"))
        body.put(if (modernOpenAi) "max_completion_tokens" else "max_tokens", 2048)
        if ((host == "dashscope.aliyuncs.com" || host.endsWith(".maas.aliyuncs.com")) && model.getString("modelName").startsWith("qwen3"))
            body.put("enable_thinking", false)
        return JSONObject().put("url", endpoint).put("method", "POST")
            .put("headers", JSONObject().put("Authorization", "Bearer $key").put("Content-Type", "application/json"))
            .put("bodyBase64", Base64.getEncoder().encodeToString(body.toString().toByteArray()))
    }
    fun decode(response: JSONObject, responsesScope: String? = null): JSONObject {
        if (response.getInt("status") !in 200..299) {
            val code = runCatching { JSONObject(String(Base64.getDecoder().decode(response.getString("bodyBase64"))))
                .optJSONObject("error")?.optString("code") }.getOrNull().orEmpty()
            val reason = when {
                code.equals("Arrearage", true) -> appText("模型账户欠费或余额不足，请检查服务账户状态", "Model account balance insufficient. Check the provider account")
                response.getInt("status") == 401 -> appText("模型 API Key 鉴权失败，请检查配置", "Model API Key authentication failed. Check settings")
                response.getInt("status") == 429 -> appText("模型请求限流或额度不足，请稍后重试并检查额度", "Model rate limit or quota reached. Check quota and try later")
                else -> "模型请求失败，请检查 API 配置、权限和额度"
            }
            error("$reason · HTTP ${response.getInt("status")}")
        }
        if (responsesScope != null) return ChatGptResponses.decode(response).put("tapilotResponsesAccount", responsesScope)
        return JSONObject(String(Base64.getDecoder().decode(response.getString("bodyBase64"))))
    }
}
