package com.kebablocator.android.services

import com.google.gson.Gson
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

object OSMService {

    private const val BASE_URL = "https://overpass-api.de/api/interpreter"

    private val client = OkHttpClient()
    private val gson = Gson()

    // ── Coroutine-based API ─────────────────────────────────────────────

    suspend fun searchKebabs(lat: Double, lon: Double, radius: Double): List<KebabShop> =
        suspendCancellableCoroutine { cont ->
            searchKebabs(lat, lon, radius) { shops -> cont.resume(shops) }
        }

    // ── Callback-based API ──────────────────────────────────────────────

    fun searchKebabs(lat: Double, lon: Double, radius: Double, callback: (List<KebabShop>) -> Unit) {
        val query = """
            [out:json];
            (
              node["amenity"="fast_food"]["cuisine"~"kebab"](around:${radius.toInt()},$lat,$lon);
              way["amenity"="fast_food"]["cuisine"~"kebab"](around:${radius.toInt()},$lat,$lon);
              node["restaurant"]["cuisine"~"kebab"](around:${radius.toInt()},$lat,$lon);
              way["restaurant"]["cuisine"~"kebab"](around:${radius.toInt()},$lat,$lon);
            );
            out body;
            >;
            out skel qt;
        """.trimIndent()

        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("data", query)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("OSMService", "searchKebabs error: ${e.message}")
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: run {
                        callback(emptyList())
                        return
                    }
                    val osmResponse = gson.fromJson(body, OSMResponse::class.java)
                    val shops = osmResponse.elements.mapNotNull { element ->
                        val name = element.tags?.get("name") ?: return@mapNotNull null
                        val elLat = element.lat ?: 0.0
                        val elLon = element.lon ?: 0.0

                        KebabShop(
                            id = "osm-${element.id}",
                            name = name,
                            rating = 4.2,
                            reviews = 10,
                            address = element.tags["addr:street"] ?: "Street unknown",
                            latitude = elLat,
                            longitude = elLon,
                            tags = listOf("Kebab", "OSM"),
                            price = "\u20AC",
                            hours = element.tags["opening_hours"] ?: "Open Now",
                            openHour = 11,
                            closeHour = 23,
                            description = "Community-sourced kebab via OpenStreetMap.",
                            imageName = "kebab_hero",
                            category = KebabCategory.DONER,
                            phone = element.tags["phone"] ?: "",
                            website = element.tags["website"] ?: "",
                            popularDishes = listOf("Döner"),
                            hasDelivery = true,
                            hasDineIn = true,
                            hasTakeaway = true
                        )
                    }
                    callback(shops)
                } catch (e: Exception) {
                    android.util.Log.e("OSMService", "Decode error: ${e.message}")
                    callback(emptyList())
                }
            }
        })
    }
}

// ── OSM API Response Data Classes ───────────────────────────────────────

data class OSMResponse(
    val elements: List<OSMElement>
)

data class OSMElement(
    val id: Long,
    val type: String,
    val lat: Double?,
    val lon: Double?,
    val tags: Map<String, String>?
)
