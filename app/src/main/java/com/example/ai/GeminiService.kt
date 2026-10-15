package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.FollowUpType
import com.example.data.model.TaskPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Converts a Bitmap into a Base64 JPEG string for Gemini multimodal calls.
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Checks if a valid non-placeholder Gemini API key is configured.
     */
    fun isKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Analyzes meeting notes, call notes, or business card data using Gemini 3.5 Flash.
     */
    suspend fun analyzeLeadInteraction(
        clientName: String,
        companyName: String,
        sourceType: AiSourceType,
        notesText: String,
        imageBitmap: Bitmap? = null
    ): GeminiAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val hasValidKey = !apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (!hasValidKey) {
            Log.w(TAG, "No valid Gemini API key configured in BuildConfig. Falling back to local smart extraction.")
            return@withContext generateSmartFallback(
                clientName = clientName,
                companyName = companyName,
                sourceType = sourceType,
                notesText = notesText,
                isDemoNotice = true
            )
        }

        try {
            val endpoint = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            // Construct system instruction and user prompt
            val systemInstruction = """
                You are an executive Sales CRM AI assistant for sales representatives.
                Your task is to analyze sales meeting notes, call logs, customer discussion transcripts, or business card details.
                You MUST extract:
                1. A concise, executive meeting summary (2-4 sentences highlighting client needs, discussion outcomes, and timeline/budget).
                2. Client sentiment (e.g. 'Positive / High Intent', 'Negotiating / Price Sensitive', 'Interested / Exploring', or 'Ready to Close').
                3. Key discussion points (bullet points of what was agreed or asked).
                4. Action items with a clear title, description, priority (HIGH, MEDIUM, LOW), and suggested due date in days (1, 2, 3, or 5).
                5. Extracted contact details if analyzing a business card (full name, title, company, phone, email, website, address).
                6. Recommended next follow-up (type: CALL, MEETING, EMAIL, or DEMO, suggested days, and clear purpose).

                Return strictly valid JSON in this exact structure without markdown backticks:
                {
                  "summary": "...",
                  "clientSentiment": "Positive / High Intent",
                  "keyDiscussionPoints": ["point 1", "point 2"],
                  "actionItems": [
                    {
                      "title": "Action item title",
                      "description": "Short explanation",
                      "priority": "HIGH",
                      "suggestedDueDateDays": 2
                    }
                  ],
                  "extractedContact": {
                    "fullName": "...",
                    "designation": "...",
                    "companyName": "...",
                    "phone": "...",
                    "email": "...",
                    "address": "...",
                    "website": "..."
                  },
                  "recommendedFollowUp": {
                    "type": "CALL",
                    "suggestedDays": 2,
                    "purpose": "..."
                  }
                }
            """.trimIndent()

            val promptText = buildString {
                append("Client Name: $clientName\n")
                append("Company: $companyName\n")
                append("Interaction Source: ${sourceType.label}\n\n")
                append("Notes & Transcription:\n$notesText\n\n")
                if (imageBitmap != null) {
                    append("Attached is the captured image of the business card/physical document. Please transcribe all text, verify contact details, and summarize.")
                }
            }

            // Build request JSON
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", promptText))

            // Multimodal inlineData if image is present
            if (imageBitmap != null) {
                val base64Data = bitmapToBase64(imageBitmap)
                val inlineDataObj = JSONObject()
                    .put("mimeType", "image/jpeg")
                    .put("data", base64Data)
                partsArray.put(JSONObject().put("inlineData", inlineDataObj))
            }

            val contentsArray = JSONArray()
            contentsArray.put(JSONObject().put("parts", partsArray))

            val requestJson = JSONObject()
            requestJson.put("contents", contentsArray)

            // System instruction
            val systemParts = JSONArray().put(JSONObject().put("text", systemInstruction))
            requestJson.put("systemInstruction", JSONObject().put("parts", systemParts))

            // Generation config
            val genConfig = JSONObject()
                .put("temperature", 0.3)
                .put("responseMimeType", "application/json")
            requestJson.put("generationConfig", genConfig)

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                Log.e(TAG, "Gemini API error ${response.code}: $responseBody")
                return@withContext generateSmartFallback(
                    clientName = clientName,
                    companyName = companyName,
                    sourceType = sourceType,
                    notesText = notesText,
                    isDemoNotice = true,
                    error = "Gemini API returned ${response.code}: Falling back to smart analysis."
                )
            }

            // Parse response candidate
            val jsonRoot = JSONObject(responseBody)
            val candidates = jsonRoot.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text")

            if (rawText.isNullOrBlank()) {
                return@withContext generateSmartFallback(
                    clientName = clientName,
                    companyName = companyName,
                    sourceType = sourceType,
                    notesText = notesText,
                    isDemoNotice = false,
                    error = "Received empty response from Gemini"
                )
            }

            parseGeminiJsonResponse(
                rawJson = rawText,
                clientName = clientName,
                companyName = companyName,
                isDemoFallback = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing Gemini API call", e)
            generateSmartFallback(
                clientName = clientName,
                companyName = companyName,
                sourceType = sourceType,
                notesText = notesText,
                isDemoNotice = true,
                error = e.localizedMessage
            )
        }
    }

    private fun parseGeminiJsonResponse(
        rawJson: String,
        clientName: String,
        companyName: String,
        isDemoFallback: Boolean,
        errorMessage: String? = null
    ): GeminiAnalysisResult {
        return try {
            // Clean markdown wrapping if present
            val cleanJson = rawJson.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val obj = JSONObject(cleanJson)
            val summary = obj.optString("summary", "Interaction analyzed successfully.")
            val clientSentiment = obj.optString("clientSentiment", "Positive / High Intent")

            val discussionPoints = mutableListOf<String>()
            val pointsArr = obj.optJSONArray("keyDiscussionPoints")
            if (pointsArr != null) {
                for (i in 0 until pointsArr.length()) {
                    val p = pointsArr.optString(i)
                    if (p.isNotBlank()) discussionPoints.add(p)
                }
            }

            val actionItems = mutableListOf<AiActionItem>()
            val actionsArr = obj.optJSONArray("actionItems")
            if (actionsArr != null) {
                for (i in 0 until actionsArr.length()) {
                    val itemObj = actionsArr.optJSONObject(i) ?: continue
                    val title = itemObj.optString("title")
                    if (title.isBlank()) continue
                    val desc = itemObj.optString("description")
                    val priorityStr = itemObj.optString("priority", "MEDIUM").uppercase()
                    val priority = when {
                        priorityStr.contains("URGENT") -> TaskPriority.URGENT
                        priorityStr.contains("HIGH") -> TaskPriority.HIGH
                        priorityStr.contains("LOW") -> TaskPriority.LOW
                        else -> TaskPriority.MEDIUM
                    }
                    val days = itemObj.optInt("suggestedDueDateDays", 2)
                    actionItems.add(
                        AiActionItem(
                            title = title,
                            description = desc,
                            priority = priority,
                            suggestedDueDateDays = days.coerceIn(1, 14)
                        )
                    )
                }
            }

            // Contact info
            var contactInfo: ExtractedContactInfo? = null
            val contactObj = obj.optJSONObject("extractedContact")
            if (contactObj != null) {
                contactInfo = ExtractedContactInfo(
                    fullName = contactObj.optString("fullName", clientName),
                    designation = contactObj.optString("designation"),
                    companyName = contactObj.optString("companyName", companyName),
                    phone = contactObj.optString("phone"),
                    email = contactObj.optString("email"),
                    address = contactObj.optString("address"),
                    website = contactObj.optString("website")
                )
            }

            // Recommended follow up
            var recommendedFollowUp: RecommendedFollowUp? = null
            val followUpObj = obj.optJSONObject("recommendedFollowUp")
            if (followUpObj != null) {
                val typeStr = followUpObj.optString("type", "CALL").uppercase()
                val type = when {
                    typeStr.contains("MEET") -> FollowUpType.MEETING
                    typeStr.contains("DEMO") -> FollowUpType.DEMO
                    typeStr.contains("EMAIL") -> FollowUpType.EMAIL
                    typeStr.contains("WHATSAPP") -> FollowUpType.WHATSAPP
                    else -> FollowUpType.CALL
                }
                val days = followUpObj.optInt("suggestedDays", 2)
                val purpose = followUpObj.optString("purpose", "Follow up on discussed proposal terms")
                recommendedFollowUp = RecommendedFollowUp(
                    type = type,
                    suggestedDays = days.coerceIn(1, 14),
                    purpose = purpose
                )
            }

            GeminiAnalysisResult(
                clientName = clientName,
                companyName = companyName,
                summary = summary,
                clientSentiment = clientSentiment,
                keyDiscussionPoints = discussionPoints,
                actionItems = actionItems,
                extractedContact = contactInfo,
                recommendedFollowUp = recommendedFollowUp,
                isDemoFallback = isDemoFallback,
                errorMessage = errorMessage
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini JSON response", e)
            generateSmartFallback(
                clientName = clientName,
                companyName = companyName,
                sourceType = AiSourceType.ALL_LEAD_DATA,
                notesText = rawJson,
                isDemoNotice = isDemoFallback,
                error = "JSON parse notice: extracted directly from response."
            )
        }
    }

    /**
     * Intelligent local fallback parser that extracts structured key points and action items
     * when the device is offline or when running in demonstration environments.
     */
    fun generateSmartFallback(
        clientName: String,
        companyName: String,
        sourceType: AiSourceType,
        notesText: String,
        isDemoNotice: Boolean = false,
        error: String? = null
    ): GeminiAnalysisResult {
        val trimmed = notesText.trim()
        val sentences = trimmed.split("\n", ".", ";").filter { it.trim().length > 5 }

        val summary = if (sentences.isNotEmpty()) {
            "Discussion with $clientName from $companyName: ${sentences.take(2).joinToString(". ").trim()}."
        } else {
            "Reviewed account details and requirements for $clientName at $companyName. Ready for immediate sales follow-up."
        }

        val sentiment = when {
            trimmed.contains("won", ignoreCase = true) || trimmed.contains("close", ignoreCase = true) || trimmed.contains("ready", ignoreCase = true) || trimmed.contains("deal", ignoreCase = true) ->
                "Ready to Close / High Intent"
            trimmed.contains("budget", ignoreCase = true) || trimmed.contains("discount", ignoreCase = true) || trimmed.contains("price", ignoreCase = true) ->
                "Price Sensitive / Negotiating"
            trimmed.contains("demo", ignoreCase = true) || trimmed.contains("interest", ignoreCase = true) ->
                "Positive / Product Interested"
            else ->
                "Neutral / Discovery Phase"
        }

        val discussionPoints = mutableListOf<String>()
        if (sentences.size > 1) {
            sentences.take(3).forEach { discussionPoints.add(it.trim()) }
        } else {
            discussionPoints.add("Evaluated requirements and scope for $companyName")
            discussionPoints.add("Verified primary decision maker details and contact timeline")
            discussionPoints.add("Discussed commercial terms and service deliverables")
        }

        val actionItems = mutableListOf<AiActionItem>()
        actionItems.add(
            AiActionItem(
                title = "Send tailored commercial proposal to $clientName",
                description = "Prepare formal quote with agreed scope and payment milestones for $companyName.",
                priority = TaskPriority.HIGH,
                suggestedDueDateDays = 2
            )
        )
        actionItems.add(
            AiActionItem(
                title = "Schedule follow-up product walkthrough",
                description = "Invite technical and procurement stakeholders from $companyName to address clarifications.",
                priority = TaskPriority.MEDIUM,
                suggestedDueDateDays = 3
            )
        )
        actionItems.add(
            AiActionItem(
                title = "Update CRM stage and share notes with account executive",
                description = "Log meeting summary and sync pipeline forecast.",
                priority = TaskPriority.LOW,
                suggestedDueDateDays = 1
            )
        )

        val recommendedFollowUp = RecommendedFollowUp(
            type = if (trimmed.contains("demo", ignoreCase = true)) FollowUpType.DEMO else FollowUpType.CALL,
            suggestedDays = 2,
            purpose = "Follow up on proposal feedback with $clientName ($companyName)"
        )

        val extractedContact = ExtractedContactInfo(
            fullName = clientName,
            companyName = companyName,
            designation = "Decision Maker",
            phone = "",
            email = "",
            address = "India",
            website = "www.${companyName.lowercase().replace(" ", "")}.com"
        )

        return GeminiAnalysisResult(
            clientName = clientName,
            companyName = companyName,
            summary = summary,
            clientSentiment = sentiment,
            keyDiscussionPoints = discussionPoints,
            actionItems = actionItems,
            extractedContact = extractedContact,
            recommendedFollowUp = recommendedFollowUp,
            isDemoFallback = isDemoNotice,
            errorMessage = error
        )
    }
}
