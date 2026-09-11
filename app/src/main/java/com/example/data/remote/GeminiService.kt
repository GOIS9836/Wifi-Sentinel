package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiOptimizationReport
import com.example.data.model.DiscoveredDevice
import com.example.data.model.NearbyAccessPoint
import com.example.data.model.WifiConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

    private val isApiKeyConfigured: Boolean
        get() = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

    suspend fun analyzeWifiAndOptimize(
        telemetry: WifiConnectionState,
        devices: List<DiscoveredDevice>,
        accessPoints: List<NearbyAccessPoint>
    ): AiOptimizationReport = withContext(Dispatchers.IO) {
        val unauthorizedCount = devices.count { !it.isAuthorized && !it.isSelf && !it.isGateway }

        if (!isApiKeyConfigured) {
            return@withContext generateLocalHeuristicReport(telemetry, devices, accessPoints, unauthorizedCount)
        }

        val prompt = """
            You are a professional Wi-Fi Network Optimization AI and Security Analyst.
            Analyze the following real-time Wi-Fi and network telemetry:
            - Connected SSID: ${telemetry.ssid} (${telemetry.securityProtocol})
            - Signal RSSI: ${telemetry.rssi} dBm (Quality: ${telemetry.signalPercent}%, Grade: ${telemetry.signalGrade.label})
            - Link Speed: ${telemetry.linkSpeedMbps} Mbps
            - Frequency: ${telemetry.frequencyMhz} MHz (Channel ${telemetry.channel}, Band: ${telemetry.band})
            - Subnet Gateway: ${telemetry.gatewayIp}
            - Discovered Devices: ${devices.size} total, with $unauthorizedCount unauthorized/untrusted devices.
            - Nearby Co-Channel & Adjacent Networks: ${accessPoints.size} visible APs.

            Provide an expert network audit formatted strictly as valid JSON with the following keys:
            {
              "overallHealthScore": <integer 0-100>,
              "summary": "<one punchy sentence summarizing network signal and threat status>",
              "optimalChannelRecommendation": "<specific channel advice for 2.4GHz and 5GHz to reduce co-channel interference>",
              "antennaAndPlacementTip": "<physical router location, elevation, and antenna angle recommendations>",
              "securityAudit": "<analysis of encryption protocol and the $unauthorizedCount unauthorized devices detected>",
              "bandSteeringAdvice": "<advice on 2.4GHz vs 5GHz/6GHz usage based on signal RSSI>",
              "actionItems": ["<Action 1>", "<Action 2>", "<Action 3>", "<Action 4>"]
            }
            Return ONLY raw JSON without markdown code fences.
        """.trimIndent()

        try {
            val jsonResponse = callGeminiApi(prompt)
            parseAiReport(jsonResponse, telemetry, devices, accessPoints, unauthorizedCount)
        } catch (e: Exception) {
            Log.e("GeminiService", "API call failed, using heuristic fallback", e)
            generateLocalHeuristicReport(telemetry, devices, accessPoints, unauthorizedCount)
        }
    }

    suspend fun askAdvisor(
        userQuestion: String,
        telemetry: WifiConnectionState,
        devices: List<DiscoveredDevice>
    ): String = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured) {
            return@withContext "To enable live Gemini AI advisor responses, please configure your GEMINI_API_KEY in the AI Studio Secrets panel.\n\n" +
                    "Quick Tip for '${userQuestion}': Current Wi-Fi is on Channel ${telemetry.channel} (${telemetry.band}) at ${telemetry.rssi} dBm. " +
                    if (telemetry.rssi < -70) "Your signal is attenuated. Consider moving closer to your router or switching to 2.4GHz for better wall penetration."
                    else "Signal strength is solid. If experiencing congestion, try changing to an uncongested channel (1, 6, 11 on 2.4GHz or 36, 48, 149 on 5GHz)."
        }

        val unauthorizedCount = devices.count { !it.isAuthorized && !it.isSelf && !it.isGateway }
        val prompt = """
            You are WiFi Sentinel AI, an expert wireless network engineer and cybersecurity advisor.
            Network Context:
            - Connected Network: ${telemetry.ssid} (Channel ${telemetry.channel}, ${telemetry.band}, ${telemetry.frequencyMhz}MHz)
            - Signal: ${telemetry.rssi} dBm, Speed: ${telemetry.linkSpeedMbps} Mbps
            - Active Devices: ${devices.size} ($unauthorizedCount unauthorized)
            
            User Question: $userQuestion

            Answer concisely in 2-3 friendly, actionable paragraphs. Focus on practical fixes for signal optimization, router config, or security mitigation.
        """.trimIndent()

        try {
            val raw = callGeminiApi(prompt)
            extractText(raw)
        } catch (e: Exception) {
            "Unable to reach Gemini AI: ${e.localizedMessage ?: "Unknown error"}. Please check your internet connection."
        }
    }

    private fun callGeminiApi(prompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val contentObj = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", prompt) })
            })
        }

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply { put(contentObj) })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("topP", 0.9)
            })
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw IllegalStateException("HTTP ${response.code}: $errorBody")
            }
            return response.body?.string() ?: ""
        }
    }

    private fun extractText(jsonStr: String): String {
        val root = JSONObject(jsonStr)
        val candidates = root.optJSONArray("candidates") ?: return "No response generated."
        val firstCandidate = candidates.optJSONObject(0) ?: return "No response generated."
        val content = firstCandidate.optJSONObject("content") ?: return "No response generated."
        val parts = content.optJSONArray("parts") ?: return "No response generated."
        val text = parts.optJSONObject(0)?.optString("text") ?: "No response generated."
        return text.trim()
    }

    private fun parseAiReport(
        jsonStr: String,
        telemetry: WifiConnectionState,
        devices: List<DiscoveredDevice>,
        accessPoints: List<NearbyAccessPoint>,
        unauthorizedCount: Int
    ): AiOptimizationReport {
        val rawText = extractText(jsonStr).replace("```json", "").replace("```", "").trim()
        return try {
            val obj = JSONObject(rawText)
            val actionArray = obj.optJSONArray("actionItems")
            val actions = mutableListOf<String>()
            if (actionArray != null) {
                for (i in 0 until actionArray.length()) {
                    actions.add(actionArray.getString(i))
                }
            }
            AiOptimizationReport(
                overallHealthScore = obj.optInt("overallHealthScore", 80),
                summary = obj.optString("summary", "Network analyzed successfully."),
                optimalChannelRecommendation = obj.optString("optimalChannelRecommendation", "Use Channel 1, 6, or 11."),
                antennaAndPlacementTip = obj.optString("antennaAndPlacementTip", "Elevate router 1.5m off the ground."),
                securityAudit = obj.optString("securityAudit", "$unauthorizedCount unauthorized devices detected."),
                bandSteeringAdvice = obj.optString("bandSteeringAdvice", "Use 5GHz for low-latency tasks."),
                actionItems = if (actions.isNotEmpty()) actions else listOf("Review unauthorized devices", "Verify router channel"),
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            generateLocalHeuristicReport(telemetry, devices, accessPoints, unauthorizedCount)
        }
    }

    private fun generateLocalHeuristicReport(
        telemetry: WifiConnectionState,
        devices: List<DiscoveredDevice>,
        accessPoints: List<NearbyAccessPoint>,
        unauthorizedCount: Int
    ): AiOptimizationReport {
        var score = 100
        val actions = mutableListOf<String>()

        // Signal penalty
        if (telemetry.rssi < -75) {
            score -= 25
            actions.add("Move closer to the router or add a Wi-Fi mesh repeater (Signal: ${telemetry.rssi} dBm)")
        } else if (telemetry.rssi < -65) {
            score -= 10
            actions.add("Relocate router to higher elevation to clear line-of-sight obstructions")
        }

        // Security penalty
        if (unauthorizedCount > 0) {
            score -= (unauthorizedCount * 12).coerceAtMost(35)
            actions.add("Block or investigate $unauthorizedCount unauthorized device(s) on subnet")
        }

        if (telemetry.securityProtocol.contains("Open", ignoreCase = true) || telemetry.securityProtocol.contains("WEP", ignoreCase = true)) {
            score -= 30
            actions.add("Upgrade router encryption immediately to WPA3-Personal or WPA2-AES")
        }

        // Channel congestion advice
        val channelAdvice = if (telemetry.band == "2.4 GHz") {
            if (telemetry.channel in listOf(1, 6, 11)) {
                "Current Channel ${telemetry.channel} is standard. If experiencing interference, test Channels ${listOf(1, 6, 11).filter { it != telemetry.channel }.joinToString(" or ")}."
            } else {
                "Channel ${telemetry.channel} suffers from overlapping non-standard interference. Switch router to non-overlapping Channel 1, 6, or 11."
            }
        } else {
            "5 GHz band detected (${telemetry.frequencyMhz} MHz). Channel ${telemetry.channel} offers wide bandwidth with minimal cross-talk."
        }

        if (telemetry.band == "2.4 GHz" && telemetry.rssi > -65) {
            actions.add("Connect to 5 GHz SSID band for lower latency and higher throughput")
        }

        return AiOptimizationReport(
            overallHealthScore = score.coerceIn(20, 100),
            summary = when {
                unauthorizedCount > 0 -> "Security Alert: $unauthorizedCount unauthorized device(s) active on ${telemetry.ssid}."
                telemetry.rssi < -70 -> "Signal Alert: High path loss detected (-${-telemetry.rssi} dBm). Optimization recommended."
                else -> "Wi-Fi running in healthy state with ${telemetry.linkSpeedMbps} Mbps link speed."
            },
            optimalChannelRecommendation = channelAdvice,
            antennaAndPlacementTip = "Elevate router at least 1.2 to 1.8 meters off the floor. Keep antennas perpendicular (one vertical, one horizontal) for dual-polarity device coverage.",
            securityAudit = if (unauthorizedCount > 0) {
                "CRITICAL: Detected $unauthorizedCount unknown device(s) accessing the network. Open Intruder Radar to verify or isolate."
            } else {
                "All ${devices.size} connected devices are recognized and authorized. Encryption protocol: ${telemetry.securityProtocol}."
            },
            bandSteeringAdvice = if (telemetry.band == "2.4 GHz") {
                "2.4 GHz penetrates walls better but has higher latency. Switch devices needing high bandwidth to 5 GHz."
            } else {
                "5 GHz provides high throughput. If moving to another room, ensure signal does not drop below -70 dBm."
            },
            actionItems = actions.ifEmpty { listOf("Network operating at peak efficiency", "Continue periodic real-time monitoring") },
            timestamp = System.currentTimeMillis()
        )
    }
}
