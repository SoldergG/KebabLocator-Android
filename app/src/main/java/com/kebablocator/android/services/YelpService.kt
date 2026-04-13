package com.kebablocator.android.services

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kebablocator.android.models.KebabCategory
import com.kebablocator.android.models.KebabShop
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.resume

object YelpService {

    private const val API_KEY = "z-CN92M6Fl60au3W0uFPGZ0TEKEHIWr0q_HbQCNNYUdfpiOm6MKZZuhl-jg5FI4QH9YzOrQNrdUs_MKal_2cdbDF_kqQlE5Qh0xnVJvTjMkg-3zezISiahtgTna4aXYx"
    private const val BASE_URL = "https://api.yelp.com/v3/businesses/search"

    private val client = OkHttpClient()
    private val gson = Gson()

    // ── Coroutine-based API ─────────────────────────────────────────────

    suspend fun searchKebabs(lat: Double, lon: Double, radius: Double): List<KebabShop> = coroutineScope {
        val page1 = async { fetchPage(lat, lon, radius, offset = 0) }
        val page2 = async { fetchPage(lat, lon, radius, offset = 50) }
        page1.await() + page2.await()
    }

    private suspend fun fetchPage(lat: Double, lon: Double, radius: Double, offset: Int): List<KebabShop> =
        suspendCancellableCoroutine { cont ->
            fetchPageCallback(lat, lon, radius, offset) { shops -> cont.resume(shops) }
        }

    // ── Callback-based API ──────────────────────────────────────────────

    fun searchKebabs(lat: Double, lon: Double, radius: Double, callback: (List<KebabShop>) -> Unit) {
        val allShops = mutableListOf<KebabShop>()
        val lock = ReentrantLock()
        val latch = CountDownLatch(2)

        for (offset in listOf(0, 50)) {
            fetchPageCallback(lat, lon, radius, offset) { shops ->
                lock.lock()
                try {
                    allShops.addAll(shops)
                } finally {
                    lock.unlock()
                }
                latch.countDown()
            }
        }

        Thread {
            latch.await()
            callback(allShops)
        }.start()
    }

    private fun fetchPageCallback(lat: Double, lon: Double, radius: Double, offset: Int, callback: (List<KebabShop>) -> Unit) {
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("term", "kebab")
            .addQueryParameter("latitude", lat.toString())
            .addQueryParameter("longitude", lon.toString())
            .addQueryParameter("radius", radius.toInt().toString())
            .addQueryParameter("limit", "50")
            .addQueryParameter("offset", offset.toString())
            .addQueryParameter("sort_by", "best_match")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $API_KEY")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("YelpService", "searchKebabs error: ${e.message}")
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: run {
                        callback(emptyList())
                        return
                    }
                    val yelpResponse = gson.fromJson(body, YelpResponse::class.java)
                    val shops = yelpResponse.businesses.map { biz ->
                        KebabShop(
                            id = biz.id,
                            name = biz.name,
                            rating = biz.rating,
                            reviews = biz.reviewCount,
                            address = biz.location.address1 ?: biz.location.city,
                            latitude = biz.coordinates.latitude,
                            longitude = biz.coordinates.longitude,
                            tags = biz.categories.map { it.title },
                            price = biz.price?.replace("$", "\u20AC") ?: "\u20AC\u20AC",
                            hours = "Open Now",
                            openHour = 11,
                            closeHour = 23,
                            description = "Authentic Kebab found via Yelp.",
                            imageName = biz.imageUrl,
                            category = KebabCategory.DONER,
                            phone = biz.displayPhone ?: "",
                            website = biz.url,
                            popularDishes = listOf("Kebab", "Fries"),
                            hasDelivery = true,
                            hasDineIn = true,
                            hasTakeaway = true
                        )
                    }
                    callback(shops)
                } catch (e: Exception) {
                    android.util.Log.e("YelpService", "Decode error: ${e.message}")
                    callback(emptyList())
                }
            }
        })
    }
}

// ── Yelp API Response Data Classes ──────────────────────────────────────

data class YelpResponse(
    val businesses: List<YelpBusiness>
)

data class YelpBusiness(
    val id: String,
    val name: String,
    @SerializedName("image_url") val imageUrl: String,
    val url: String,
    @SerializedName("review_count") val reviewCount: Int,
    val categories: List<YelpCategory>,
    val rating: Double,
    val coordinates: YelpCoordinates,
    val location: YelpLocation,
    @SerializedName("display_phone") val displayPhone: String?,
    val price: String?
)

data class YelpCategory(
    val alias: String,
    val title: String
)

data class YelpCoordinates(
    val latitude: Double,
    val longitude: Double
)

data class YelpLocation(
    val address1: String?,
    val city: String,
    @SerializedName("zip_code") val zipCode: String,
    val country: String
)
