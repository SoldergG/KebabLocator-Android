package com.kebablocator.android.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kebablocator.android.models.KebabShop
import com.kebablocator.android.models.SupabaseShopDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    // MARK: - State

    private val _favoriteIDs = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIDs: StateFlow<Set<String>> = _favoriteIDs.asStateFlow()

    private val _allShops = MutableStateFlow<List<KebabShop>>(emptyList())
    val allShops: StateFlow<List<KebabShop>> = _allShops.asStateFlow()

    private val _verifiedPlaceIDs = MutableStateFlow<Set<String>>(emptySet())
    val verifiedPlaceIDs: StateFlow<Set<String>> = _verifiedPlaceIDs.asStateFlow()

    private val _reportedPlaceIDs = MutableStateFlow<Set<String>>(emptySet())
    val reportedPlaceIDs: StateFlow<Set<String>> = _reportedPlaceIDs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // MARK: - Supabase Config

    private val supabaseUrl = "https://omwvsocbyqqriictjyfx.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9td3Zzb2NieXFxcmlpY3RqeWZ4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzM3NTU3NjMsImV4cCI6MjA4OTMzMTc2M30.621TXLX7f3oYQ0wMI6W_-fAG8041TwOVilLASvlqT-8"

    // MARK: - Private

    private val prefs: SharedPreferences =
        application.getSharedPreferences("kebab_locator_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val favoritesKey = "kebab_favorites"
    private val cachedShopsKey = "cached_supabase_shops"
    private val cachedShopsTimestampKey = "cached_supabase_shops_timestamp"
    private val verifiedKey = "verified_place_ids"
    private val reportedKey = "reported_place_ids"
    private val cacheDurationMs = 5 * 60 * 1000L // 5 minutes

    private var lastFetchTimestamp: Long = 0L

    init {
        loadFavorites()
        loadVerifiedIDs()
        loadReportedIDs()
        loadCachedShops()
    }

    // MARK: - Favorites

    fun isFavorite(id: String): Boolean {
        return _favoriteIDs.value.contains(id)
    }

    fun toggleFavorite(shop: KebabShop) {
        val currentFavorites = _favoriteIDs.value.toMutableSet()
        if (currentFavorites.contains(shop.id)) {
            currentFavorites.remove(shop.id)
        } else {
            currentFavorites.add(shop.id)
        }
        _favoriteIDs.value = currentFavorites
        saveFavorites()
        triggerHapticFeedback()
    }

    fun getFavoriteShops(allShops: List<KebabShop>): List<KebabShop> {
        val combined = _allShops.value + allShops
        // De-duplicate by id, preferring Supabase entries (from _allShops)
        val seen = mutableSetOf<String>()
        val unique = mutableListOf<KebabShop>()
        for (shop in combined) {
            if (shop.id !in seen) {
                seen.add(shop.id)
                unique.add(shop)
            }
        }
        return unique.filter { it.id in _favoriteIDs.value }
    }

    // MARK: - Fetch Shops from Supabase

    fun fetchShops(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceRefresh &&
            (now - lastFetchTimestamp) < cacheDurationMs &&
            _allShops.value.isNotEmpty()
        ) {
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val shops = withContext(Dispatchers.IO) {
                    fetchShopsFromSupabase()
                }
                _allShops.value = shops
                lastFetchTimestamp = System.currentTimeMillis()
                cacheShops()
            } catch (e: Exception) {
                _errorMessage.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchShopsFromSupabase(): List<KebabShop> {
        val urlString =
            "$supabaseUrl/rest/v1/kebab_shops?select=*&is_active=eq.true&order=is_sponsored.desc,rating.desc"
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("apikey", supabaseKey)
            connection.setRequestProperty("Authorization", "Bearer $supabaseKey")
            connection.setRequestProperty("Content-Type", "application/json")

            if (connection.responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().readText()
                val type = object : TypeToken<List<SupabaseShopDTO>>() {}.type
                val dtoList: List<SupabaseShopDTO> = gson.fromJson(response, type)
                dtoList.map { it.toKebabShop() }
            } else {
                throw Exception("Server returned ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    // MARK: - Submit Shop

    fun submitShop(
        dto: SupabaseShopDTO,
        imageBytes: ByteArray?,
        callback: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Upload image first if provided
                var imageUrl: String? = null
                if (imageBytes != null) {
                    imageUrl = withContext(Dispatchers.IO) {
                        uploadPhoto(imageBytes)
                    }
                }

                // Create the shop DTO with image URL
                val shopWithImage = dto.copy(image_url = imageUrl)

                val success = withContext(Dispatchers.IO) {
                    postShopToSupabase(shopWithImage)
                }

                if (success) {
                    callback(true, null)
                } else {
                    callback(false, "Server error")
                }
            } catch (e: Exception) {
                callback(false, e.localizedMessage)
            }
        }
    }

    private fun postShopToSupabase(shop: SupabaseShopDTO): Boolean {
        val url = URL("$supabaseUrl/rest/v1/kebab_shops")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("apikey", supabaseKey)
            connection.setRequestProperty("Authorization", "Bearer $supabaseKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Prefer", "return=minimal")
            connection.doOutput = true

            val json = gson.toJson(shop)
            OutputStreamWriter(connection.outputStream).use { it.write(json) }

            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    }

    private fun uploadPhoto(imageBytes: ByteArray): String? {
        val fileName = "${UUID.randomUUID()}.jpg"
        val url = URL("$supabaseUrl/storage/v1/object/shop-photos/$fileName")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("apikey", supabaseKey)
            connection.setRequestProperty("Authorization", "Bearer $supabaseKey")
            connection.setRequestProperty("Content-Type", "image/jpeg")
            connection.doOutput = true

            connection.outputStream.use { it.write(imageBytes) }

            if (connection.responseCode in 200..299) {
                "$supabaseUrl/storage/v1/object/public/shop-photos/$fileName"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    // MARK: - Verification

    fun submitVerification(placeId: String, callback: (Boolean) -> Unit) {
        if (_verifiedPlaceIDs.value.contains(placeId)) {
            callback(false)
            return
        }

        val isInternal = _allShops.value.any { it.id == placeId }

        viewModelScope.launch {
            try {
                if (isInternal) {
                    val success = withContext(Dispatchers.IO) {
                        callVerificationRPC(placeId)
                    }
                    if (success) {
                        markAsVerified(placeId)
                    }
                    callback(success)
                } else {
                    // For external shops, mark as verified locally
                    markAsVerified(placeId)
                    callback(true)
                }
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    private fun callVerificationRPC(placeId: String): Boolean {
        val url = URL("$supabaseUrl/rest/v1/rpc/submit_place_verification")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("apikey", supabaseKey)
            connection.setRequestProperty("Authorization", "Bearer $supabaseKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val body = JSONObject().apply {
                put("p_place_id", placeId)
                put("p_submitter_id", JSONObject.NULL)
            }
            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

            connection.responseCode in 200..299
        } catch (e: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }

    // MARK: - Reports

    fun submitReport(placeId: String, reason: String, description: String, callback: (Boolean) -> Unit) {
        if (_reportedPlaceIDs.value.contains(placeId)) {
            callback(false)
            return
        }

        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    postReport(placeId, reason, description)
                }
                if (success) {
                    markAsReported(placeId)
                }
                callback(success)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    private fun postReport(placeId: String, reason: String, description: String): Boolean {
        val url = URL("$supabaseUrl/rest/v1/place_reports")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("apikey", supabaseKey)
            connection.setRequestProperty("Authorization", "Bearer $supabaseKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Prefer", "return=minimal")
            connection.doOutput = true

            val body = JSONObject().apply {
                put("place_id", placeId)
                put("reason", reason)
                put("description", description)
            }
            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

            connection.responseCode in 200..299
        } catch (e: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }

    // MARK: - Verified / Reported Persistence

    private fun markAsVerified(shopId: String) {
        val updated = _verifiedPlaceIDs.value.toMutableSet()
        updated.add(shopId)
        _verifiedPlaceIDs.value = updated
        saveSetToPrefs(verifiedKey, updated)
    }

    private fun markAsReported(shopId: String) {
        val updated = _reportedPlaceIDs.value.toMutableSet()
        updated.add(shopId)
        _reportedPlaceIDs.value = updated
        saveSetToPrefs(reportedKey, updated)
    }

    // MARK: - Haptic Feedback

    private fun triggerHapticFeedback() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Haptic feedback is non-critical
        }
    }

    // MARK: - SharedPreferences Persistence

    private fun saveFavorites() {
        saveSetToPrefs(favoritesKey, _favoriteIDs.value)
    }

    private fun loadFavorites() {
        _favoriteIDs.value = loadSetFromPrefs(favoritesKey)
    }

    private fun loadVerifiedIDs() {
        _verifiedPlaceIDs.value = loadSetFromPrefs(verifiedKey)
    }

    private fun loadReportedIDs() {
        _reportedPlaceIDs.value = loadSetFromPrefs(reportedKey)
    }

    private fun saveSetToPrefs(key: String, set: Set<String>) {
        try {
            val json = gson.toJson(set)
            prefs.edit().putString(key, json).apply()
        } catch (e: Exception) {
            // Silently fail
        }
    }

    private fun loadSetFromPrefs(key: String): Set<String> {
        return try {
            val json = prefs.getString(key, null) ?: return emptySet()
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptySet()
        }
    }

    // MARK: - Shop Caching

    private fun cacheShops() {
        try {
            val json = gson.toJson(_allShops.value)
            prefs.edit()
                .putString(cachedShopsKey, json)
                .putLong(cachedShopsTimestampKey, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            // Silently fail
        }
    }

    private fun loadCachedShops() {
        try {
            val json = prefs.getString(cachedShopsKey, null) ?: return
            val type = object : TypeToken<List<KebabShop>>() {}.type
            val shops: List<KebabShop> = gson.fromJson(json, type)
            _allShops.value = shops
            lastFetchTimestamp = prefs.getLong(cachedShopsTimestampKey, 0L)
        } catch (e: Exception) {
            // Silently fail
        }
    }
}
