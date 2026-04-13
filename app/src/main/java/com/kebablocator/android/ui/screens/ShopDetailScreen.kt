package com.kebablocator.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kebablocator.android.models.KebabShop
import com.kebablocator.android.ui.components.BannerAd
import com.kebablocator.android.ui.components.GlassPill
import com.kebablocator.android.ui.components.glassCard
import com.kebablocator.android.ui.theme.KebabColors
import com.kebablocator.android.utils.*
import com.kebablocator.android.viewmodels.FavoritesViewModel

@Composable
fun ShopDetailScreen(
    shop: KebabShop,
    favoritesVM: FavoritesViewModel,
    userLat: Double,
    userLon: Double,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isFavorite by remember(favoritesVM.favoriteIDs) {
        derivedStateOf { favoritesVM.isFavorite(shop.id) }
    }

    var showReportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KebabColors.bgPrimary)
    ) {
        // Header Image
        item {
            Box(modifier = Modifier.height(250.dp).fillMaxWidth()) {
                val imageUrl = shop.imageUrl ?: shop.imageName
                if (imageUrl.startsWith("http")) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = shop.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(KebabColors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(shop.category.emoji, fontSize = 64.sp)
                    }
                }

                // Back button overlay
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .background(KebabColors.bgPrimary.copy(alpha = 0.6f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = KebabColors.textPrimary)
                }
            }
        }

        // Tags
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shop.tags) { tag ->
                    GlassPill(text = tag)
                }
            }
        }

        // Name + Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${shop.category.emoji} ${shop.name}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = KebabColors.textPrimary
                    )
                }
                IconButton(onClick = { favoritesVM.toggleFavorite(shop) }) {
                    Icon(
                        if (isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        null,
                        tint = if (isFavorite) KebabColors.accentOrange else KebabColors.textMuted
                    )
                }
                IconButton(onClick = { context.shareShop(shop.name, shop.address, shop.rating) }) {
                    Icon(Icons.Filled.Share, null, tint = KebabColors.textMuted)
                }
            }
        }

        // Rating + Status
        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stars
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { i ->
                        Icon(
                            Icons.Filled.Star,
                            null,
                            tint = if (i < shop.rating.toInt()) KebabColors.starYellow
                                   else KebabColors.textMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${"%.1f".format(shop.rating)} (${shop.reviews})",
                        fontSize = 14.sp,
                        color = KebabColors.textSecondary
                    )
                }

                // Status badge
                Surface(
                    color = if (shop.isOpenNow) KebabColors.openGreen.copy(alpha = 0.15f)
                            else KebabColors.closedRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = shop.statusText,
                        color = if (shop.isOpenNow) KebabColors.openGreen else KebabColors.closedRed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Info Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .glassCard()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow(Icons.Filled.LocationOn, shop.address)
                if (shop.phone.isNotBlank()) InfoRow(Icons.Filled.Phone, shop.phone)
                if (shop.website.isNotBlank()) InfoRow(Icons.Filled.Language, shop.website)
                InfoRow(Icons.Filled.Schedule, "${shop.hours} (${shop.openHour}:00 - ${shop.closeHour}:00)")
                InfoRow(Icons.Filled.AttachMoney, shop.price)

                val distance = shop.distanceFrom(userLat, userLon)
                if (userLat != 0.0) InfoRow(Icons.Filled.NearMe, distance.formatDistance())
            }
        }

        // Services
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (shop.hasDelivery) ServiceChip("🚴 Delivery")
                if (shop.hasDineIn) ServiceChip("🪑 Dine-In")
                if (shop.hasTakeaway) ServiceChip("📦 Takeaway")
            }
        }

        // Popular Dishes
        if (shop.popularDishes.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .glassCard()
                        .padding(16.dp)
                ) {
                    Text("Popular Dishes", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    shop.popularDishes.forEach { dish ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("•", color = KebabColors.accentOrange, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(dish, color = KebabColors.textSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Description
        if (shop.description.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .glassCard()
                        .padding(16.dp)
                ) {
                    Text("About", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(shop.description, color = KebabColors.textSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }

        // Action Buttons
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Directions
                Button(
                    onClick = { context.openDirections(shop.latitude, shop.longitude, shop.name) },
                    colors = ButtonDefaults.buttonColors(containerColor = KebabColors.accentOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Directions, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get Directions", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (shop.phone.isNotBlank()) {
                        OutlinedButton(
                            onClick = { context.openPhone(shop.phone) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KebabColors.textPrimary)
                        ) {
                            Icon(Icons.Filled.Call, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call")
                        }
                    }
                    if (shop.website.isNotBlank()) {
                        OutlinedButton(
                            onClick = { context.openWebsite(shop.website) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KebabColors.textPrimary)
                        ) {
                            Icon(Icons.Filled.Language, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Website")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            favoritesVM.submitVerification(shop.id) {}
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Verified, null, tint = KebabColors.openGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verify", color = KebabColors.openGreen)
                    }
                    TextButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Flag, null, tint = KebabColors.closedRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report", color = KebabColors.closedRed)
                    }
                }
            }
        }

        // Banner Ad
        item {
            BannerAd(modifier = Modifier.padding(16.dp))
        }

        // Verification status
        if (shop.isVerified) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(KebabColors.openGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Verified, null, tint = KebabColors.openGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verified by community", color = KebabColors.openGreen, fontSize = 13.sp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    // Report Dialog
    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, desc ->
                favoritesVM.submitReport(shop.id, reason, desc) {}
                showReportDialog = false
            }
        )
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = KebabColors.accentOrange, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = KebabColors.textSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun ServiceChip(text: String) {
    Surface(
        color = KebabColors.accentOrange.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = KebabColors.accentOrange,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val reasons = listOf("Permanently Closed", "Wrong Location", "Duplicate", "Inappropriate", "Other")
    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Place", color = KebabColors.textPrimary) },
        containerColor = KebabColors.bgElevated,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedReason == reason) KebabColors.accentOrange.copy(alpha = 0.1f)
                                else KebabColors.surface.copy(alpha = 0.5f)
                            )
                            .padding(12.dp)
                            .then(Modifier.clickable { selectedReason = reason })
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = KebabColors.accentOrange)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(reason, color = KebabColors.textPrimary, fontSize = 14.sp)
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Additional details...", color = KebabColors.textMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = KebabColors.textPrimary,
                        unfocusedTextColor = KebabColors.textPrimary,
                        focusedBorderColor = KebabColors.accentOrange,
                        unfocusedBorderColor = KebabColors.textMuted.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selectedReason, description) }) {
                Text("Submit", color = KebabColors.closedRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = KebabColors.textMuted)
            }
        }
    )
}
