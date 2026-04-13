package com.kebablocator.android.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kebablocator.android.models.*
import com.kebablocator.android.ui.components.BannerAd
import com.kebablocator.android.ui.components.GlassPill
import com.kebablocator.android.ui.components.KebabShopCard
import com.kebablocator.android.ui.theme.KebabColors
import com.kebablocator.android.viewmodels.FavoritesViewModel
import com.kebablocator.android.viewmodels.LocationViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    locationVM: LocationViewModel,
    favoritesVM: FavoritesViewModel,
    onShopClick: (KebabShop) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(FilterOption.ALL) }
    var selectedSort by remember { mutableStateOf(SortOption.DISTANCE) }
    var showSortMenu by remember { mutableStateOf(false) }

    val liveShops by locationVM.liveShops.collectAsState()
    val supabaseShops by favoritesVM.allShops.collectAsState()
    val userLocation by locationVM.userLocation.collectAsState()

    // Easter egg shake animation
    val shakeOffset = remember { Animatable(0f) }
    val isEasterEgg = searchText == "67"

    LaunchedEffect(isEasterEgg) {
        if (isEasterEgg) {
            repeat(5) {
                shakeOffset.animateTo(10f, tween(50))
                shakeOffset.animateTo(-10f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    val allShops = remember(liveShops, supabaseShops) {
        (liveShops + supabaseShops).distinctBy { it.id }
    }

    val filteredShops = remember(allShops, searchText, selectedFilter, selectedSort, userLocation) {
        var shops = allShops

        // Text search
        if (searchText.isNotBlank() && searchText != "67") {
            val query = searchText.lowercase()
            shops = shops.filter { shop ->
                shop.name.lowercase().contains(query) ||
                shop.address.lowercase().contains(query) ||
                shop.tags.any { it.lowercase().contains(query) } ||
                shop.description.lowercase().contains(query) ||
                shop.popularDishes.any { it.lowercase().contains(query) }
            }
        }

        // Filter
        shops = when (selectedFilter) {
            FilterOption.ALL -> shops
            FilterOption.OPEN_NOW -> shops.filter { it.isOpenNow }
            FilterOption.TOP_RATED -> shops.filter { it.rating >= 4.5 }
            FilterOption.DONER -> shops.filter { it.category == KebabCategory.DONER }
            FilterOption.FALAFEL -> shops.filter { it.category == KebabCategory.FALAFEL }
            FilterOption.DURUM -> shops.filter { it.category == KebabCategory.DURUM }
            FilterOption.SHAWARMA -> shops.filter { it.category == KebabCategory.SHAWARMA }
            FilterOption.LATE_NIGHT -> shops.filter { it.closeHour in 1..6 || it.closeHour == 24 }
            FilterOption.DELIVERY -> shops.filter { it.hasDelivery }
            FilterOption.BUDGET -> shops.filter { it.price == "€" }
            FilterOption.DINE_IN -> shops.filter { it.hasDineIn }
        }

        // Sort
        val uLat = userLocation?.latitude ?: 0.0
        val uLon = userLocation?.longitude ?: 0.0
        when (selectedSort) {
            SortOption.DISTANCE -> shops.sortedBy { it.distanceFrom(uLat, uLon) }
            SortOption.RATING -> shops.sortedByDescending { it.rating }
            SortOption.PRICE -> shops.sortedBy { it.price.length }
            SortOption.NAME -> shops.sortedBy { it.name }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KebabColors.bgPrimary)
            .statusBarsPadding()
    ) {
        // Header
        Text(
            text = "Explore",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = KebabColors.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search kebabs...", color = KebabColors.textMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = KebabColors.textMuted) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Filled.Close, null, tint = KebabColors.textMuted)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = KebabColors.textPrimary,
                unfocusedTextColor = KebabColors.textPrimary,
                focusedBorderColor = KebabColors.accentOrange,
                unfocusedBorderColor = KebabColors.textMuted.copy(alpha = 0.3f),
                cursorColor = KebabColors.accentOrange,
                focusedContainerColor = KebabColors.bgElevated,
                unfocusedContainerColor = KebabColors.bgElevated
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterOption.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KebabColors.accentOrange.copy(alpha = 0.2f),
                        selectedLabelColor = KebabColors.accentOrange,
                        containerColor = KebabColors.bgElevated,
                        labelColor = KebabColors.textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = KebabColors.textMuted.copy(alpha = 0.2f),
                        selectedBorderColor = KebabColors.accentOrange.copy(alpha = 0.5f),
                        enabled = true,
                        selected = selectedFilter == filter
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sort Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredShops.size} results",
                fontSize = 13.sp,
                color = KebabColors.textMuted
            )

            Box {
                TextButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Filled.Sort, null, tint = KebabColors.accentOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(selectedSort.displayName, color = KebabColors.accentOrange, fontSize = 13.sp)
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    containerColor = KebabColors.bgElevated
                ) {
                    SortOption.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort.displayName, color = KebabColors.textPrimary) },
                            onClick = {
                                selectedSort = sort
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Results List
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredShops, key = { it.id }) { shop ->
                KebabShopCard(
                    shop = shop,
                    userLat = userLocation?.latitude ?: 0.0,
                    userLon = userLocation?.longitude ?: 0.0,
                    isFavorite = favoritesVM.isFavorite(shop.id),
                    onFavoriteClick = { favoritesVM.toggleFavorite(shop) },
                    onClick = { onShopClick(shop) }
                )
            }

            item {
                BannerAd(modifier = Modifier.padding(vertical = 8.dp))
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
