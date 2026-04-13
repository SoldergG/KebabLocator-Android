package com.kebablocator.android.services

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kebablocator.android.models.KebabCategory
import com.kebablocator.android.models.KebabShop
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume

object FoursquareService {

    // NOTE: Foursquare requires an API Key.
    // Get one at: https://location.foursquare.com/developer/
    private const val API_KEY = "fsq3YOUR_KEY_HERE"
    private const val BASE_URL = "https://api.foursquare.com/v3/places/search"

    private val client = OkHttpClient()
    private val gson = Gson()

    // ── Coroutine-based API ─────────────────────────────────────────────

    suspend fun searchKebabs(lat: Double, lon: Double, radius: Double): List<KebabShop> =
        suspendCancellableCoroutine { cont ->
            searchKebabs(lat, lon, radius) { shops -> cont.resume(shops) }
        }

    // ── Callback-based API ──────────────────────────────────────────────

    fun searchKebabs(lat: Double, lon: Double, radius: Double, callback: (List<KebabShop>) -> Unit) {
        // If user hasn't provided a key, return empty
        if (API_KEY == "fsq3YOUR_KEY_HERE") {
            callback(emptyList())
            return
        }

        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("ll", "$lat,$lon")
            .addQueryParameter("query", "kebab")
            .addQueryParameter("categories", "13000") // Dining and Drinking
            .addQueryParameter("radius", radius.toInt().toString())
            .addQueryParameter("limit", "30")
            .addQueryParameter("fields", "fsq_id,name,rating,stats,location,geocodes,photos,price,tel,website,hours")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", API_KEY)
            .addHeader("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("FoursquareService", "searchKebabs error: ${e.message}")
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: run {
                        callback(emptyList())
                        return
                    }
                    val fsqResponse = gson.fromJson(body, FoursquareResponse::class.java)
                    val shops = fsqResponse.results.map { result ->
                        val photoPrefix = result.photos?.firstOrNull()?.prefix
                        val photoSuffix = result.photos?.firstOrNull()?.suffix
                        val imageUrl = if (photoPrefix != null && photoSuffix != null) {
                            "${photoPrefix}original${photoSuffix}"
                        } else ""

                        KebabShop(
                            id = "fsq-${result.fsqId}",
                            name = result.name,
                            rating = (result.rating ?: 80.0) / 20.0, // Foursquare 0-10 -> 0-5
                            reviews = result.stats?.totalRatings ?: 0,
                            address = result.location.address ?: result.location.formattedAddress ?: "Unknown",
                            latitude = result.geocodes.main.latitude,
                            longitude = result.geocodes.main.longitude,
                            tags = listOf("Kebab", "Foursquare"),
                            price = "\u20AC".repeat(result.price ?: 2),
                            hours = result.hours?.display ?: "Open Now",
                            openHour = 11,
                            closeHour = 23,
                            description = "Kebab spot verified via Foursquare.",
                            imageName = imageUrl,
                            category = KebabCategory.DONER,
                            phone = result.tel ?: "",
                            website = result.website ?: "",
                            popularDishes = listOf("Mixed Kebab"),
                            hasDelivery = true,
                            hasDineIn = true,
                            hasTakeaway = true
                        )
                    }
                    callback(shops)
                } catch (e: Exception) {
                    android.util.Log.e("FoursquareService", "Decode error: ${e.message}")
                    callback(emptyList())
                }
            }
        })
    }
}

// ── Foursquare API Response Data Classes ────────────────────────────────

data class FoursquareResponse(
    val results: List<FoursquareResult>
)

data class FoursquareResult(
    @SerializedName("fsq_id") val fsqId: String,
    val name: String,
    val rating: Double?,
    val stats: FoursquareStats?,
    val location: FoursquareLocation,
    val geocodes: FoursquareGeocodes,
    val photos: List<FoursquarePhoto>?,
    val price: Int?,
    val tel: String?,
    val website: String?,
    val hours: FoursquareHours?
)

data class FoursquareStats(
    @SerializedName("total_ratings") val totalRatings: Int?
)

data class FoursquareLocation(
    val address: String?,
    @SerializedName("formatted_address") val formattedAddress: String?
)

data class FoursquareGeocodes(
    val main: FoursquareLatLng
)

data class FoursquareLatLng(
    val latitude: Double,
    val longitude: Double
)

data class FoursquarePhoto(
    val prefix: String,
    val suffix: String
)

data class FoursquareHours(
    val display: String?
)
