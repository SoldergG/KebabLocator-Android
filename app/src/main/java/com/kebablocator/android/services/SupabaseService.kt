package com.kebablocator.android.services

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kebablocator.android.models.KebabShop
import com.kebablocator.android.models.SupabaseShopDTO
import com.kebablocator.android.models.toKebabShop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume

object SupabaseService {

    private const val SUPABASE_URL = "https://omwvsocbyqqriictjyfx.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9td3Zzb2NieXFxcmlpY3RqeWZ4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzM3NTU3NjMsImV4cCI6MjA4OTMzMTc2M30.621TXLX7f3oYQ0wMI6W_-fAG8041TwOVilLASvlqT-8"

    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val jpegMediaType = "image/jpeg".toMediaType()

    // ── Coroutine-based API ─────────────────────────────────────────────

    suspend fun fetchShops(): List<KebabShop> = suspendCancellableCoroutine { cont ->
        fetchShops { shops -> cont.resume(shops) }
    }

    suspend fun uploadPhoto(imageBytes: ByteArray): String? = suspendCancellableCoroutine { cont ->
        uploadPhoto(imageBytes) { url -> cont.resume(url) }
    }

    suspend fun submitShop(dto: SupabaseShopDTO): Boolean = suspendCancellableCoroutine { cont ->
        submitShop(dto) { success -> cont.resume(success) }
    }

    suspend fun submitVerification(placeId: String): Boolean = suspendCancellableCoroutine { cont ->
        submitVerification(placeId) { success -> cont.resume(success) }
    }

    suspend fun submitReport(placeId: String, reason: String, description: String): Boolean =
        suspendCancellableCoroutine { cont ->
            submitReport(placeId, reason, description) { success -> cont.resume(success) }
        }

    // ── Callback-based API ──────────────────────────────────────────────

    fun fetchShops(callback: (List<KebabShop>) -> Unit) {
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/kebab_shops?select=*&order=is_sponsored.desc")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("SupabaseService", "fetchShops error: ${e.message}")
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: run {
                        callback(emptyList())
                        return
                    }
                    val type = object : TypeToken<List<SupabaseShopDTO>>() {}.type
                    val dtos: List<SupabaseShopDTO> = gson.fromJson(body, type)
                    val shops = dtos.map { it.toKebabShop() }
                    callback(shops)
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseService", "fetchShops decode error: ${e.message}")
                    callback(emptyList())
                }
            }
        })
    }

    fun uploadPhoto(imageBytes: ByteArray, callback: (String?) -> Unit) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val requestBody = imageBytes.toRequestBody(jpegMediaType)

        val request = Request.Builder()
            .url("$SUPABASE_URL/storage/v1/object/shop-photos/$fileName")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "image/jpeg")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("SupabaseService", "uploadPhoto error: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val publicUrl = "$SUPABASE_URL/storage/v1/object/public/shop-photos/$fileName"
                    callback(publicUrl)
                } else {
                    android.util.Log.e("SupabaseService", "uploadPhoto failed: ${response.code}")
                    callback(null)
                }
            }
        })
    }

    fun submitShop(dto: SupabaseShopDTO, callback: (Boolean) -> Unit) {
        val json = gson.toJson(dto)
        val requestBody = json.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/kebab_shops")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("SupabaseService", "submitShop error: ${e.message}")
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }

    fun submitVerification(placeId: String, callback: (Boolean) -> Unit) {
        val json = gson.toJson(mapOf("place_id" to placeId))
        val requestBody = json.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/rpc/submit_place_verification")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("SupabaseService", "submitVerification error: ${e.message}")
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }

    fun submitReport(placeId: String, reason: String, description: String, callback: (Boolean) -> Unit) {
        val json = gson.toJson(
            mapOf(
                "place_id" to placeId,
                "reason" to reason,
                "description" to description
            )
        )
        val requestBody = json.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/place_reports")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("SupabaseService", "submitReport error: ${e.message}")
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }
}
