package lifeos.md.tapilot.conversation

import java.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NativeModelRequestTest {
    @Test fun modernChatModelsUseCompletionBudgetOnlyAtOfficialEndpoint() {
        val profile = ModelProfiles.defaults("openai").put("modelName", "gpt-5")
        fun body() = JSONObject(String(Base64.getDecoder().decode(
            NativeModelRequest.params(profile, "test-key", JSONArray()).getString("bodyBase64"))))
        assertEquals(2048, body().getInt("max_completion_tokens"))
        assertFalse(body().has("max_tokens"))
        profile.put("baseUrl", "https://example.org/v1")
        assertEquals(2048, body().getInt("max_tokens"))
        assertFalse(body().has("max_completion_tokens"))
    }
    @Test fun terraToolsUseResponsesAndReplayIsIsolatedByCredentialAndModel() {
        val profile = ModelProfiles.defaults("openai").put("modelName", "gpt-6.1-terra")
        val scope = NativeModelRequest.responsesScope(profile, "test-key")
        val messages = JSONArray().put(JSONObject().put("role", "assistant").put("content", "Working")
            .put("tapilotResponsesAccount", scope).put("tapilotResponsesOutput",
                JSONArray().put(JSONObject().put("type", "reasoning").put("encrypted_content", "opaque"))))
        val request = NativeModelRequest.params(profile, "test-key", messages)
        assertEquals("https://api.openai.com/v1/responses", request.getString("url"))
        fun body(key: String) = JSONObject(String(Base64.getDecoder().decode(
            NativeModelRequest.params(profile, key, messages).getString("bodyBase64"))))
        assertTrue(body("test-key").toString().contains("opaque"))
        assertFalse(body("replacement-key").toString().contains("opaque"))
        assertFalse(body("test-key").has("reasoning_effort"))
        assertFalse(body("test-key").has("max_tokens"))
        assertEquals("namespace", body("test-key").getJSONArray("tools").getJSONObject(0).getString("type"))
        profile.put("modelName", "gpt-6-astra")
        assertFalse(body("test-key").toString().contains("opaque"))
        assertTrue(messages.toString().contains("opaque"))
    }
    @Test fun switchingFromChatGptDoesNotForwardOpaqueResponsesStateToOtherProviders() {
        val messages = JSONArray().put(JSONObject().put("role", "assistant").put("content", "done")
            .put("tapilotResponsesAccount", "account").put("tapilotResponsesOutput", JSONArray().put("opaque")))
        val request = NativeModelRequest.params(ModelProfiles.defaults("deepseek"), "test-key", messages)
        val body = String(Base64.getDecoder().decode(request.getString("bodyBase64")))
        assertFalse(body.contains("tapilotResponses")); assertFalse(body.contains("opaque"))
        assertTrue(messages.getJSONObject(0).has("tapilotResponsesOutput"))
    }

    @Test fun qwenRequestDisablesDefaultReasoningWithoutChangingStoredConfiguration() {
        val profile = ModelProfiles.defaults("qwen")
        val request = NativeModelRequest.params(profile, "test-key", JSONArray())
        val body = JSONObject(String(Base64.getDecoder().decode(request.getString("bodyBase64"))))
        assertFalse(body.getBoolean("enable_thinking"))
        assertFalse(profile.has("enable_thinking"))
        assertEquals("Bearer test-key", request.getJSONObject("headers").getString("Authorization"))
        assertEquals("operate_phone", body.getJSONArray("tools").getJSONObject(0).getJSONObject("function").getString("name"))
    }
    @Test fun otherProvidersDoNotReceiveQwenExtension() {
        val request = NativeModelRequest.params(ModelProfiles.defaults("openai"), "test-key", JSONArray())
        val body = JSONObject(String(Base64.getDecoder().decode(request.getString("bodyBase64"))))
        assertFalse(body.has("enable_thinking"))
    }
    @Test fun errorResponseDoesNotExposeItsBody() {
        val response = JSONObject().put("status", 401).put("bodyBase64", Base64.getEncoder().encodeToString("private credential".toByteArray()))
        val error = assertThrows(IllegalStateException::class.java) { NativeModelRequest.decode(response) }
        assertTrue(error.message!!.contains("401"))
        assertFalse(error.message!!.contains("private"))
    }
    @Test fun arrearageIsDistinctFromInvalidConfiguration() {
        val errorBody = JSONObject().put("error", JSONObject().put("code", "Arrearage").put("message", "private service message"))
        val response = JSONObject().put("status", 400).put("bodyBase64", Base64.getEncoder().encodeToString(errorBody.toString().toByteArray()))
        val error = assertThrows(IllegalStateException::class.java) { NativeModelRequest.decode(response) }
        assertTrue(error.message!!.contains("balance insufficient"))
        assertFalse(error.message!!.contains("private"))
    }
}
