package com.kebablocator.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kebablocator.android.models.KebabShop
import com.kebablocator.android.models.SortOption
import com.kebablocator.android.ui.components.BannerAd
import com.kebablocator.android.ui.components.GlassStatCard
import com.kebablocator.android.ui.components.KebabShopCard
import com.kebablocator.android.ui.theme.KebabColors
import com.kebablocator.android.viewmodels.FavoritesViewModel
import com.kebablocator.android.viewmodels.LocationViewModel

@Composable
fun FavoritesScreen(
    locationVM: LocationViewModel,
    favoritesVM: FavoritesViewModel,
    onShopClick: (KebabShop) -> Unit
) {
    val favoriteIDs by favoritesVM.favoriteIDs.collectAsState()
    val allShops by favoritesVM.allShops.collectAsState()
    val liveShops by locationVM.liveShops.collectAsState()
    val userLocation by locationVM.userLocation.collectAsState()

    var selectedSort by remember { mutableStateOf(SortOption.RATING) }

    val favoriteShops = remember(favoriteIDs, allShops, liveShops, selectedSort, userLocation) {
        val combined = (allShops + liveShops).distinctBy { it.id }
        val favs = combined.filter { it.id in favoriteIDs }

        val uLat = userLocation?.latitude ?: 0.0
        val uLon = userLocation?.longitude ?: 0.0
        when (selectedSort) {
            SortOption.DISTANCE -> favs.sortedBy { it.distanceFrom(uLat, uLon) }
            SortOption.RATING -> favs.sortedByDescending { it.rating }
            SortOption.PRICE -> favs.sortedBy { it.price.length }
            SortOption.NAME -> favs.sortedBy { it.name }
        }
    }

    val avgRating = if (favoriteShops.isNotEmpty()) favoriteShops.map { it.rating }.average() else 0.0
    val uniqueCategories = favoriteShops.map { it.category }.distinct().size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KebabColors.bgPrimary)
            .statusBarsPadding()
    ) {
        Text(
            text = "Saved",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = KebabColors.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        if (favoriteShops.isEmpty()) {
            // Empty state
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.BookmarkBorder,
                        null,
                        tint = KebabColors.textMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No favorites yet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = KebabColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the bookmark icon on any kebab shop to save it here.",
                        fontSize = 14.sp,
                        color = KebabColors.textMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stats row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassStatCard(
                            icon = Icons.Filled.Star,
                            value = "%.1f".format(avgRating),
                            label = "Avg Rating",
                            color = KebabColors.starYellow,
                            modifier = Modifier.weight(1f)
                        )
                        GlassStatCard(
                            icon = Icons.Filled.Category,
                            value = "$uniqueCategories",
                            label = "Categories",
                            color = KebabColors.accentOrange,
                            modifier = Modifier.weight(1f)
                        )
                        GlassStatCard(
                            icon = Icons.Filled.Bookmark,
                            value = "${favoriteShops.size}",
                            label = "Saved",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Sort options
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortOption.entries.forEach { sort ->
                            FilterChip(
                                selected = selectedSort == sort,
                                onClick = { selectedSort = sort },
                                label = { Text(sort.displayName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KebabColors.accentOrange.copy(alpha = 0.2f),
                                    selectedLabelColor = KebabColors.accentOrange,
                                    containerColor = KebabColors.bgElevated,
                                    labelColor = KebabColors.textSecondary
                                )
                            )
                        }
                    }
                }

                item { BannerAd() }

                items(favoriteShops, key = { it.id }) { shop ->
                    KebabShopCard(
                        shop = shop,
                        userLat = userLocation?.latitude ?: 0.0,
                        userLon = userLocation?.longitude ?: 0.0,
                        isFavorite = true,
                        onFavoriteClick = { favoritesVM.toggleFavorite(shop) },
                        onClick = { onShopClick(shop) }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
