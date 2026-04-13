package com.kebablocator.android.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.location.Location
import kotlin.math.*

// Distance calculation using Android's built-in Location class
fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0].toDouble() / 1000.0 // km
}

// Open Google Maps directions
fun Context.openDirections(lat: Double, lon: Double, name: String) {
    val uri = Uri.parse("google.navigation:q=$lat,$lon&label=${Uri.encode(name)}")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        // Fallback to browser
        val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lon")
        startActivity(Intent(Intent.ACTION_VIEW, browserUri))
    }
}

// Open phone dialer
fun Context.openPhone(number: String) {
    if (number.isNotBlank()) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        startActivity(intent)
    }
}

// Open website
fun Context.openWebsite(url: String) {
    if (url.isNotBlank()) {
        val fullUrl = if (url.startsWith("http")) url else "https://$url"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
        startActivity(intent)
    }
}

// Share shop
fun Context.shareShop(name: String, address: String, rating: Double) {
    val text = "Check out $name! ⭐ ${"%.1f".format(rating)} - $address\n\nFound via Kebab Locator 🥙"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, "Share $name"))
}

// Format distance
fun Double.formatDistance(): String {
    return when {
        this < 0.1 -> "${(this * 1000).toInt()}m"
        this < 10 -> "${"%.1f".format(this)}km"
        else -> "${this.toInt()}km"
    }
}

// Format rating as stars
fun Double.toStarString(): String {
    val fullStars = this.toInt()
    val hasHalf = this - fullStars >= 0.5
    val emptyStars = 5 - fullStars - if (hasHalf) 1 else 0
    return "★".repeat(fullStars) + (if (hasHalf) "½" else "") + "☆".repeat(emptyStars)
}
