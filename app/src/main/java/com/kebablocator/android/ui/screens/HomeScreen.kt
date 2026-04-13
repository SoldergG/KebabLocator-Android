package com.kebablocator.android.ui.screens

import android.location.Location
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kebablocator.android.models.KebabShop
import com.kebablocator.android.ui.components.BannerAd
import com.kebablocator.android.ui.components.GlassBackground
import com.kebablocator.android.ui.components.GlassPill
import com.kebablocator.android.ui.components.KebabShopCard
import com.kebablocator.android.ui.components.glassCard
import com.kebablocator.android.ui.theme.KebabColors
import com.kebablocator.android.viewmodels.FavoritesViewModel
import com.kebablocator.android.viewmodels.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    locationVM: LocationViewModel,
    favoritesVM: FavoritesViewModel,
    onShopClick: (KebabShop) -> Unit,
    onAddShopClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val liveShops by locationVM.liveShops.collectAsState()
    val convenienceStores by locationVM.convenienceStores.collectAsState()
    val userLocation by locationVM.userLocation.collectAsState()
    val isSearching by locationVM.isSearching.collectAsState()
    val favoriteIDs by favoritesVM.favoriteIDs.collectAsState()

    var selectedMode by remember { mutableIntStateOf(0) } // 0 = Kebab, 1 = Convenience

    val shops = if (selectedMode == 0) liveShops else convenienceStores
    val isLoading = isSearching

    // Sort by distance
    val uLat = userLocation?.latitude ?: 0.0
    val uLon = userLocation?.longitude ?: 0.0
    val sortedShops = remember(shops, userLocation) {
        if (uLat == 0.0 && uLon == 0.0) shops
        else shops.sortedBy { it.distanceFrom(uLat, uLon) }
    }

    val topPicks = remember(sortedShops) {
        sortedShops.filter { it.rating >= 4.0 }.take(5)
    }

    val allPlaces = remember(sortedShops) {
        sortedShops.take(15)
    }

    // Fetch Supabase shops on first load
    LaunchedEffect(Unit) {
        favoritesVM.fetchShops()
    }

    GlassBackground(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = {
                if (selectedMode == 0) locationVM.searchNearbyKebabs()
                else locationVM.searchNearbyConvenienceStores()
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            text = "Kebab Locator",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = KebabColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Find the best kebabs near you",
                            fontSize = 14.sp,
                            color = KebabColors.textSecondary
                        )
                    }
                }

                // Mode toggle
                item {
                    ModeToggle(
                        selectedMode = selectedMode,
                        onModeChange = { mode ->
                            selectedMode = mode
                            if (mode == 1) locationVM.searchNearbyConvenienceStores()
                        },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                // Quick actions
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            icon = Icons.Filled.Add,
                            label = "Add Shop",
                            onClick = onAddShopClick,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            icon = Icons.Filled.Settings,
                            label = "Settings",
                            onClick = onSettingsClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Banner Ad
                item {
                    BannerAd(modifier = Modifier.padding(horizontal = 20.dp))
                }

                // Top Picks Section
                if (topPicks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Top Picks",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = KebabColors.textPrimary,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(topPicks, key = { it.id }) { shop ->
                                TopPickCard(
                                    shop = shop,
                                    userLocation = userLocation,
                                    onClick = { onShopClick(shop) }
                                )
                            }
                        }
                    }
                }

                // All Places Section
                item {
                    Text(
                        text = "All Places",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = KebabColors.textPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                items(allPlaces, key = { it.id }) { shop ->
                    KebabShopCard(
                        shop = shop,
                        userLocation = userLocation,
                        isFavorite = favoriteIDs.contains(shop.id),
                        onFavoriteClick = { favoritesVM.toggleFavorite(shop) },
                        onClick = { onShopClick(shop) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                // Empty state
                if (allPlaces.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No shops found nearby.\nTry increasing the search radius.",
                                fontSize = 14.sp,
                                color = KebabColors.textMuted,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(
    selectedMode: Int,
    onModeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf("Kebab", "Convenience Store")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 12.dp)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        modes.forEachIndexed { index, label ->
            val isSelected = selectedMode == index
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) KebabColors.accentOrange else KebabColors.bgElevated.copy(alpha = 0f),
                animationSpec = tween(300),
                label = "modeBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) KebabColors.bgPrimary else KebabColors.textSecondary,
                animationSpec = tween(300),
                label = "modeText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable { onModeChange(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .glassCard(cornerRadius = 12.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = KebabColors.accentOrange,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = KebabColors.textPrimary
        )
    }
}

@Composable
private fun TopPickCard(
    shop: KebabShop,
    userLocation: Location?,
    onClick: () -> Unit
) {
    val distanceKm = userLocation?.let {
        shop.distanceFrom(it.latitude, it.longitude)
    }
    val distanceText = distanceKm?.let {
        if (it < 0.1) "${(it * 1000).toInt()}m" else String.format("%.1fkm", it)
    } ?: ""

    Column(
        modifier = Modifier
            .width(160.dp)
            .glassCard(cornerRadius = 14.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = shop.imageUrl ?: shop.imageName,
            contentDescription = shop.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
        )
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = shop.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = KebabColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = KebabColors.starYellow,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = String.format("%.1f", shop.rating),
                    fontSize = 11.sp,
                    color = KebabColors.textSecondary
                )
                if (distanceText.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = distanceText,
                        fontSize = 11.sp,
                        color = KebabColors.textMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                shop.tags.take(2).forEach { tag ->
                    GlassPill(text = tag)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (shop.isOpenNow) "Open" else "Closed",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (shop.isOpenNow) KebabColors.openGreen else KebabColors.closedRed
            )
        }
    }
}
