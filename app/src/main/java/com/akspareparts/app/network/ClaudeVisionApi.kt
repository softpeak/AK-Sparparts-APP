package com.akspareparts.app.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.akspareparts.app.data.ExtractedPart
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

/**
 * Calls the Anthropic Claude vision API to extract part numbers + prices from an image.
 * Endpoint: POST https://api.anthropic.com/v1/messages
 */
object ClaudeVisionApi {

    private const val URL = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-sonnet-4-20250514"
    private const val ANTHROPIC_VERSION = "2023-06-01"
    private const val PROMPT =
        "Extract all part numbers and prices from this image. " +
        "Return JSON array: [{partNumber: string, price: number}]"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Success(val parts: List<ExtractedPart>) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun extractParts(context: Context, imageUri: Uri, apiKey: String): Result =
        withContext(Dispatchers.IO) {
            try {
                val (base64, mediaType) = encodeImage(context, imageUri)
                    ?: return@withContext Result.Error("Could not read the selected image.")

                val body = buildRequestBody(base64, mediaType)
                val request = Request.Builder()
                    .url(URL)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", ANTHROPIC_VERSION)
                    .addHeader("content-type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { resp ->
                    val respBody = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.Error("API error ${resp.code}: ${shorten(respBody)}")
                    }
                    val parts = parseResponse(respBody)
                    if (parts.isEmpty()) Result.Error("No parts found in the image.")
                    else Result.Success(parts)
                }
            } catch (e: Exception) {
                Result.Error(e.message ?: "Unknown error")
            }
        }

    private fun buildRequestBody(base64: String, mediaType: String): JSONObject {
        val imageSource = JSONObject()
            .put("type", "base64")
            .put("media_type", mediaType)
            .put("data", base64)
        val imageBlock = JSONObject().put("type", "image").put("source", imageSource)
        val textBlock = JSONObject().put("type", "text").put("text", PROMPT)
        val content = JSONArray().put(imageBlock).put(textBlock)
        val message = JSONObject().put("role", "user").put("content", content)
        return JSONObject()
            .put("model", MODEL)
            .put("max_tokens", 1000)
            .put("messages", JSONArray().put(message))
    }

    /** Extracts the JSON array from Claude's text response (tolerates surrounding prose). */
    private fun parseResponse(json: String): List<ExtractedPart> {
        val root = JSONObject(json)
        val contentArr = root.optJSONArray("content") ?: return emptyList()
        val sb = StringBuilder()
        for (i in 0 until contentArr.length()) {
            val block = contentArr.getJSONObject(i)
            if (block.optString("type") == "text") sb.append(block.optString("text"))
        }
        val text = sb.toString()
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start == -1 || end == -1 || end < start) return emptyList()
        val arr = JSONArray(text.substring(start, end + 1))
        val out = mutableListOf<ExtractedPart>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val pn = o.optString("partNumber").trim()
            val price = o.optDouble("price", 0.0)
            if (pn.isNotEmpty()) out.add(ExtractedPart(pn, price))
        }
        return out
    }

    /** Loads + downsizes the image, returns base64 + media type (always JPEG here). */
    private fun encodeImage(context: Context, uri: Uri): Pair<String, String>? {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = input.use { BitmapFactory.decodeStream(it) } ?: return null
        val scaled = downscale(original, 1568)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        return b64 to "image/jpeg"
    }

    private fun downscale(bmp: Bitmap, maxDim: Int): Bitmap {
        val w = bmp.width; val h = bmp.height
        val largest = maxOf(w, h)
        if (largest <= maxDim) return bmp
        val ratio = maxDim.toFloat() / largest
        return Bitmap.createScaledBitmap(bmp, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    private fun shorten(s: String) = if (s.length > 200) s.substring(0, 200) + "..." else s
}
