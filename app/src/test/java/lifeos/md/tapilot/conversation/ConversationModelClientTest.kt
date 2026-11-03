package lifeos.md.tapilot.conversation

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean

class ConversationModelClientTest {
    @Test fun officialApiResponsesReturnsToolCallsAndCredentialScopedReplay() {
        val profile = ModelProfile("https://api.openai.com/v1", "gpt-6.1-terra", "gpt-6")
        val client = ConversationModelClient({ error("API-key flow must not access ChatGPT") }, { params, _, _ ->
            assertEquals("https://api.openai.com/v1/responses", params.getString("url"))
            assertEquals("Bearer api-key", params.getJSONObject("headers").getString("Authorization"))
            val body = """data: {"type":"response.completed","response":{"status":"completed","output":[{"type":"function_call","namespace":"tapilot","call_id":"call-1","name":"answer_question","arguments":"{}"}]}}

"""
            JSONObject().put("status", 200).put("bodyBase64", Base64.getEncoder().encodeToString(body.toByteArray()))
        })
        val result = client.complete(profile, "api-key", JSONArray(), JSONArray(), "session", AtomicBoolean(false))
        assertEquals("answer_question", result.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            .getJSONArray("tool_calls").getJSONObject(0).getJSONObject("function").getString("name"))
        assertEquals(NativeModelRequest.responsesScope(profile.json(), "api-key"), result.getString("tapilotResponsesAccount"))
        assertFalse(result.toString().contains("api-key"))
    }
    @Test fun diagnosticsCannotFailOrRepeatACompletion() {
        val phases = mutableListOf<Triple<String, Long, Boolean>>()
        var requests = 0
        val client = ConversationModelClient({ "private-token" }, { params, _, _ ->
            requests++
            assertEquals("Bearer private-token", params.getJSONObject("headers").getString("Authorization"))
            val body = """data: {"type":"response.completed","response":{"status":"completed","output":[{"type":"message","content":[{"type":"output_text","text":"OK"}]}]}}

"""
            JSONObject().put("status", 200).put("bodyBase64", Base64.getEncoder().encodeToString(body.toByteArray()))
        }, onTiming = { phase, duration, success ->
            phases.add(Triple(phase, duration, success))
            throw IllegalStateException("diagnostic failure")
        })
        val result = client.chatGpt("account", "model", JSONArray(), JSONArray(), "session", AtomicBoolean(false))
        assertEquals("OK", result.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"))
        assertEquals(1, requests)
        assertEquals(listOf("credentials", "encode", "request", "decode"), phases.map { it.first })
        assertTrue(phases.all { it.second >= 0 && it.third })
    }

    @Test fun credentialFailureIsMeasuredWithoutIssuingARequest() {
        val original = IllegalStateException("credential failure")
        val events = mutableListOf<Pair<String, Boolean>>()
        var requests = 0
        val client = ConversationModelClient({ throw original }, { _, _, _ -> requests++; JSONObject() },
            onTiming = { phase, _, success -> events.add(phase to success) })
        val failure = assertThrows(IllegalStateException::class.java) {
            client.chatGpt("account", "model", JSONArray(), JSONArray(), "session", AtomicBoolean(false))
        }
        assertSame(original, failure)
        assertEquals(0, requests)
        assertEquals(listOf("credentials" to false), events)
    }
}
