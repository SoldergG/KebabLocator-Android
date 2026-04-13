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

object GooglePlacesService {

    private const val API_KEY = "AIzaSyBH0cZIc5Wkspk7Mvmuqfqizk6niK8gh-o"
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"

    private val client = OkHttpClient()
    private val gson = Gson()

    // ── Coroutine-based API ─────────────────────────────────────────────

    suspend fun searchKebabs(lat: Double, lon: Double, radius: Double, pageToken: String? = null): List<KebabShop> =
        suspendCancellableCoroutine { cont ->
            searchKebabs(lat, lon, radius, pageToken) { shops -> cont.resume(shops) }
        }

    suspend fun searchConvenienceStores(lat: Double, lon: Double, radius: Double, pageToken: String? = null): List<KebabShop> =
        suspendCancellableCoroutine { cont ->
            searchConvenienceStores(lat, lon, radius, pageToken) { shops -> cont.resume(shops) }
        }

    // ── Callback-based API ──────────────────────────────────────────────

    fun searchKebabs(lat: Double, lon: Double, radius: Double, pageToken: String? = null, callback: (List<KebabShop>) -> Unit) {
        searchPlaces(lat, lon, radius, keyword = "kebab", type = "restaurant", pageToken = pageToken, callback = callback)
    }

    fun searchConvenienceStores(lat: Double, lon: Double, radius: Double, pageToken: String? = null, callback: (List<KebabShop>) -> Unit) {
        searchPlaces(lat, lon, radius, keyword = "convenience store", type = "convenience_store", pageToken = pageToken, callback = callback)
    }

    private fun searchPlaces(
        lat: Double,
        lon: Double,
        radius: Double,
        keyword: String,
        type: String,
        pageToken: String? = null,
        callback: (List<KebabShop>) -> Unit
    ) {
        val urlBuilder = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("location", "$lat,$lon")
            .addQueryParameter("radius", radius.toInt().toString())
            .addQueryParameter("keyword", keyword)
            .addQueryParameter("type", type)
            .addQueryParameter("key", API_KEY)

        if (pageToken != null) {
            urlBuilder.addQueryParameter("pagetoken", pageToken)
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("GooglePlacesService", "searchPlaces error: ${e.message}")
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: run {
                        callback(emptyList())
                        return
                    }
                    val googleResponse = gson.fromJson(body, GoogleResponse::class.java)

                    if (googleResponse.status != "OK" && googleResponse.status != "ZERO_RESULTS") {
                        android.util.Log.e("GooglePlacesService", "API status: ${googleResponse.status}, message: ${googleResponse.errorMessage}")
                        callback(emptyList())
                        return
                    }

                    val isConvenience = type == "convenience_store"
                    val shops = googleResponse.results.map { result ->
                        val photoRef = result.photos?.firstOrNull()?.photoReference
                        val imageUrl = if (photoRef != null) {
                            "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photoreference=$photoRef&key=$API_KEY"
                        } else ""

                        val category = if (isConvenience) KebabCategory.MIXED else KebabCategory.DONER
                        val tags = if (isConvenience) listOf("Convenience", "Google", "All Night") else listOf("Kebab", "Google")

                        KebabShop(
                            id = result.placeId,
                            name = result.name,
                            rating = result.rating ?: 4.0,
                            reviews = result.userRatingsTotal ?: 0,
                            address = result.vicinity ?: "Unknown Address",
                            latitude = result.geometry.location.lat,
                            longitude = result.geometry.location.lng,
                            tags = tags,
                            price = "\u20AC".repeat(result.priceLevel ?: 2),
                            hours = "Open Now",
                            openHour = 0,
                            closeHour = 24,
                            description = if (isConvenience) "Convenience store found via Google." else "Found via Google Places.",
                            imageName = imageUrl,
                            category = category,
                            phone = "",
                            website = "",
                            popularDishes = if (isConvenience) listOf("Snacks", "Drinks") else listOf("Döner"),
                            hasDelivery = true,
                            hasDineIn = !isConvenience,
                            hasTakeaway = true
                        )
                    }
                    callback(shops)
                } catch (e: Exception) {
                    android.util.Log.e("GooglePlacesService", "Decode error: ${e.message}")
                    callback(emptyList())
                }
            }
        })
    }
}

// ── Google API Response Data Classes ────────────────────────────────────

data class GoogleResponse(
    val status: String,
    @SerializedName("error_message") val errorMessage: String?,
    val results: List<GoogleResult>,
    @SerializedName("next_page_token") val nextPageToken: String?
)

data class GoogleResult(
    @SerializedName("place_id") val placeId: String,
    val name: String,
    val rating: Double?,
    @SerializedName("user_ratings_total") val userRatingsTotal: Int?,
    val vicinity: String?,
    @SerializedName("price_level") val priceLevel: Int?,
    val geometry: GoogleGeometry,
    val photos: List<GooglePhoto>?
)

data class GoogleGeometry(
    val location: GoogleLocation
)

data class GoogleLocation(
    val lat: Double,
    val lng: Double
)

data class GooglePhoto(
    @SerializedName("photo_reference") val photoReference: String
)
