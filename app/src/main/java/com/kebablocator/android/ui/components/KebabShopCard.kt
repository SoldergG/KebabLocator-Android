package com.kebablocator.android.ui.components

import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kebablocator.android.models.KebabShop
import com.kebablocator.android.ui.theme.KebabColors

// Overload with lat/lon
@Composable
fun KebabShopCard(
    shop: KebabShop,
    userLat: Double,
    userLon: Double,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loc = if (userLat != 0.0 || userLon != 0.0) {
        Location("").apply { latitude = userLat; longitude = userLon }
    } else null
    KebabShopCard(shop, loc, isFavorite, onFavoriteClick, onClick, modifier)
}

@Composable
fun KebabShopCard(
    shop: KebabShop,
    userLocation: Location?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distance = userLocation?.let {
        val results = FloatArray(1)
        Location.distanceBetween(it.latitude, it.longitude, shop.latitude, shop.longitude, results)
        results[0].toDouble()
    }
    val distanceText = distance?.let {
        if (it < 1000) "${it.toInt()}m" else String.format("%.1fkm", it / 1000)
    } ?: ""

    val categoryEmoji = shop.category.emoji

    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        AsyncImage(
            model = shop.imageUrl ?: shop.imageName,
            contentDescription = shop.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            // Name row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = shop.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KebabColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Open/Closed badge
                Text(
                    text = if (shop.isOpenNow) "Open" else "Closed",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (shop.isOpenNow) KebabColors.openGreen else KebabColors.closedRed
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Rating row
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (index < shop.rating.toInt()) KebabColors.starYellow
                        else KebabColors.textMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f", shop.rating),
                    fontSize = 12.sp,
                    color = KebabColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Address + distance
            Text(
                text = shop.address,
                fontSize = 12.sp,
                color = KebabColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom row: category, price, distance
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${shop.category.emoji} ${shop.category.displayName}",
                    fontSize = 11.sp,
                    color = KebabColors.textSecondary
                )
                if (shop.price.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = shop.price,
                        fontSize = 11.sp,
                        color = KebabColors.accentOrange
                    )
                }
                if (distanceText.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = distanceText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = KebabColors.textSecondary
                    )
                }
            }
        }

        // Favorite button
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) KebabColors.accentOrange else KebabColors.textMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
