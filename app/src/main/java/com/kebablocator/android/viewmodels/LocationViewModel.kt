package com.kebablocator.android.viewmodels

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kebablocator.android.models.KebabShop
import com.kebablocator.android.services.ConvenienceStoreService
import com.kebablocator.android.services.FoursquareService
import com.kebablocator.android.services.GooglePlacesService
import com.kebablocator.android.services.OSMService
import com.kebablocator.android.services.YelpService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class LocationViewModel : ViewModel() {

    // MARK: - State

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _isUsingManualLocation = MutableStateFlow(false)
    val isUsingManualLocation: StateFlow<Boolean> = _isUsingManualLocation.asStateFlow()

    private val _manualAddress = MutableStateFlow("")
    val manualAddress: StateFlow<String> = _manualAddress.asStateFlow()

    private val _liveShops = MutableStateFlow<List<KebabShop>>(emptyList())
    val liveShops: StateFlow<List<KebabShop>> = _liveShops.asStateFlow()

    private val _convenienceStores = MutableStateFlow<List<KebabShop>>(emptyList())
    val convenienceStores: StateFlow<List<KebabShop>> = _convenienceStores.asStateFlow()

    private val _searchRadius = MutableStateFlow(10.0) // km
    val searchRadius: StateFlow<Double> = _searchRadius.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isFetchingConvenienceStores = MutableStateFlow(false)
    val isFetchingConvenienceStores: StateFlow<Boolean> = _isFetchingConvenienceStores.asStateFlow()

    private val _cameraPosition = MutableStateFlow<LatLng?>(null)
    val cameraPosition: StateFlow<LatLng?> = _cameraPosition.asStateFlow()

    // Default: Lisbon
    private val defaultLatLng = LatLng(38.7167, -9.1395)

    val effectiveLocation: Location
        get() = _userLocation.value ?: Location("default").apply {
            latitude = defaultLatLng.latitude
            longitude = defaultLatLng.longitude
        }

    // MARK: - Private

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var lastSearchLocation: Location? = null
    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    private val cachedLiveShopsKey = "cached_live_shops"

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val SEARCH_DISTANCE_THRESHOLD = 500f // meters
        private const val DEDUP_DISTANCE_THRESHOLD = 50.0 // meters
    }

    // MARK: - Initialization

    fun initialize(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        prefs = context.getSharedPreferences("kebab_locator_prefs", Context.MODE_PRIVATE)
        loadCachedLiveShops()
        // Trigger initial search at default location
        searchNearbyKebabs()
    }

    // MARK: - Permissions

    fun requestPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates(activity)
        }
    }

    // MARK: - Location Updates

    fun startLocationUpdates(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L // 10 seconds
        ).setMinUpdateIntervalMillis(5_000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (_isUsingManualLocation.value) return

                _userLocation.value = location
                _cameraPosition.value = LatLng(location.latitude, location.longitude)

                // Only search if moved >500m or first search
                val lastSearch = lastSearchLocation
                if (lastSearch == null || location.distanceTo(lastSearch) > SEARCH_DISTANCE_THRESHOLD) {
                    lastSearchLocation = location
                    searchNearbyKebabs()
                }
            }
        }

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
        }
    }

    // MARK: - Manual Location

    fun setManualLocation(lat: Double, lon: Double, address: String) {
        _isUsingManualLocation.value = true
        _manualAddress.value = address
        val location = Location("manual").apply {
            latitude = lat
            longitude = lon
        }
        _userLocation.value = location
        _cameraPosition.value = LatLng(lat, lon)
        searchNearbyKebabs()
    }

    fun useGPSLocation() {
        _isUsingManualLocation.value = false
        _manualAddress.value = ""
        // Location updates will resume via the callback
    }

    fun setSearchRadius(km: Double) {
        _searchRadius.value = km
        searchNearbyKebabs()
    }

    fun setCameraPosition(latLng: LatLng) {
        _cameraPosition.value = latLng
    }

    // MARK: - Geocoding

    fun reverseGeocode(context: Context, lat: Double, lon: Double, callback: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val address = addresses?.firstOrNull()
                if (address != null) {
                    val parts = listOfNotNull(
                        address.thoroughfare,
                        address.subThoroughfare,
                        address.locality,
                        address.postalCode,
                        address.countryName
                    )
                    callback(parts.joinToString(", "))
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                callback(null)
            }
        }
    }

    fun geocodeAddress(context: Context, address: String, callback: (LatLng?) -> Unit) {
        viewModelScope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(address, 1)
                val location = results?.firstOrNull()
                if (location != null) {
                    callback(LatLng(location.latitude, location.longitude))
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                callback(null)
            }
        }
    }

    // MARK: - Nearby Kebab Search

    fun searchNearbyKebabs() {
        val location = effectiveLocation
        val radiusMeters = _searchRadius.value * 1000

        _isSearching.value = true

        viewModelScope.launch {
            try {
                val lat = location.latitude
                val lon = location.longitude

                // Call all 4 services in parallel
                val results = listOf(
                    async {
                        try {
                            YelpService.searchKebabs(lat, lon, radiusMeters)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    },
                    async {
                        try {
                            GooglePlacesService.searchKebabs(lat, lon, radiusMeters)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    },
                    async {
                        try {
                            OSMService.searchKebabs(lat, lon, radiusMeters)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    },
                    async {
                        try {
                            FoursquareService.searchKebabs(lat, lon, radiusMeters)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                ).awaitAll().flatten()

                // De-duplicate against existing shops
                val existingShops = _liveShops.value.toMutableList()
                for (shop in results) {
                    val isDuplicate = existingShops.any { existing ->
                        isDuplicateShop(existing, shop)
                    }
                    if (!isDuplicate) {
                        existingShops.add(shop)
                    }
                }

                _liveShops.value = existingShops
                cacheLiveShops()
            } catch (e: Exception) {
                // Keep existing cached data on error
            } finally {
                _isSearching.value = false
            }
        }
    }

    // MARK: - Nearby Convenience Store Search

    fun searchNearbyConvenienceStores() {
        val location = effectiveLocation
        val radiusMeters = _searchRadius.value * 1000

        _isFetchingConvenienceStores.value = true

        viewModelScope.launch {
            try {
                val lat = location.latitude
                val lon = location.longitude

                val results = listOf(
                    async {
                        try {
                            ConvenienceStoreService.searchConvenienceStores(lat, lon, radiusMeters)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    },
                    async {
                        try {
                            GooglePlacesService.searchConvenienceStores(lat, lon, radiusMeters)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                ).awaitAll().flatten()

                // Filter out sample stores if real ones exist
                val hasRealStores = results.any { !it.id.startsWith("sample-") && !it.id.startsWith("conv_") }

                val filtered = if (hasRealStores) {
                    results.filter { !it.id.startsWith("sample-") }
                } else {
                    results
                }

                // De-duplicate
                val uniqueStores = mutableListOf<KebabShop>()
                for (store in filtered) {
                    val isDuplicate = uniqueStores.any { existing ->
                        isDuplicateShop(existing, store)
                    }
                    if (!isDuplicate) {
                        uniqueStores.add(store)
                    }
                }

                // Sort by distance from current location
                _convenienceStores.value = uniqueStores.sortedBy { shop ->
                    distanceBetween(
                        location.latitude, location.longitude,
                        shop.latitude, shop.longitude
                    )
                }
            } catch (e: Exception) {
                // Keep existing data on error
            } finally {
                _isFetchingConvenienceStores.value = false
            }
        }
    }

    // MARK: - Deduplication

    private fun isDuplicateShop(existing: KebabShop, candidate: KebabShop): Boolean {
        val nameMatch = existing.name.equals(candidate.name, ignoreCase = true) ||
                existing.name.lowercase().contains(candidate.name.lowercase()) ||
                candidate.name.lowercase().contains(existing.name.lowercase())

        val distance = distanceBetween(
            existing.latitude, existing.longitude,
            candidate.latitude, candidate.longitude
        )

        return nameMatch && distance < DEDUP_DISTANCE_THRESHOLD
    }

    /**
     * Calculates distance between two coordinates in meters using the Haversine-like formula.
     */
    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    // MARK: - Caching

    private fun cacheLiveShops() {
        try {
            val json = gson.toJson(_liveShops.value)
            prefs?.edit()?.putString(cachedLiveShopsKey, json)?.apply()
        } catch (e: Exception) {
            // Silently fail cache write
        }
    }

    private fun loadCachedLiveShops() {
        try {
            val json = prefs?.getString(cachedLiveShopsKey, null) ?: return
            val type = object : TypeToken<List<KebabShop>>() {}.type
            val shops: List<KebabShop> = gson.fromJson(json, type)
            _liveShops.value = shops
        } catch (e: Exception) {
            // Silently fail cache read
        }
    }

    // MARK: - Cleanup

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
