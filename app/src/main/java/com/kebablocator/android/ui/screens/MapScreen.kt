package com.kebablocator.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.kebablocator.android.models.KebabShop
import com.kebablocator.android.ui.components.BannerAd
import com.kebablocator.android.ui.components.GlassPill
import com.kebablocator.android.ui.theme.KebabColors
import com.kebablocator.android.utils.formatDistance
import com.kebablocator.android.viewmodels.FavoritesViewModel
import com.kebablocator.android.viewmodels.LocationViewModel

// Dark map style JSON
private val darkMapStyle = """
[
  {"elementType":"geometry","stylers":[{"color":"#212121"}]},
  {"elementType":"labels.icon","stylers":[{"visibility":"off"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#757575"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#212121"}]},
  {"featureType":"administrative","elementType":"geometry","stylers":[{"color":"#757575"}]},
  {"featureType":"poi","elementType":"geometry","stylers":[{"color":"#181818"}]},
  {"featureType":"road","elementType":"geometry.fill","stylers":[{"color":"#2c2c2c"}]},
  {"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#212121"}]},
  {"featureType":"road.highway","elementType":"geometry.fill","stylers":[{"color":"#3c3c3c"}]},
  {"featureType":"transit","elementType":"geometry","stylers":[{"color":"#2f3948"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#000000"}]}
]
""".trimIndent()

@Composable
fun MapScreen(
    locationVM: LocationViewModel,
    favoritesVM: FavoritesViewModel,
    onShopClick: (KebabShop) -> Unit
) {
    val liveShops by locationVM.liveShops.collectAsState()
    val supabaseShops by favoritesVM.allShops.collectAsState()
    val userLocation by locationVM.userLocation.collectAsState()

    val allShops = remember(liveShops, supabaseShops) {
        (liveShops + supabaseShops).distinctBy { it.id }
    }

    var selectedShop by remember { mutableStateOf<KebabShop?>(null) }

    val defaultPosition = LatLng(
        userLocation?.latitude ?: 38.7223,
        userLocation?.longitude ?: -9.1393
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 14f)
    }

    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 14f)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = userLocation != null,
                mapStyleOptions = MapStyleOptions(darkMapStyle)
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            )
        ) {
            allShops.forEach { shop ->
                Marker(
                    state = MarkerState(position = LatLng(shop.latitude, shop.longitude)),
                    title = shop.name,
                    snippet = "${shop.category.emoji} ${shop.rating}★",
                    onClick = {
                        selectedShop = shop
                        true
                    }
                )
            }
        }

        // Banner Ad at top
        BannerAd(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .statusBarsPadding()
                .align(Alignment.TopCenter)
        )

        // My Location button
        FloatingActionButton(
            onClick = {
                userLocation?.let {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f)
                    )
                }
            },
            containerColor = KebabColors.bgElevated,
            contentColor = KebabColors.accentOrange,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (selectedShop != null) 200.dp else 100.dp)
        ) {
            Icon(Icons.Filled.MyLocation, "My Location")
        }

        // Selected shop card at bottom
        selectedShop?.let { shop ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
                    .clickable { onShopClick(shop) },
                colors = CardDefaults.cardColors(containerColor = KebabColors.bgElevated),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${shop.category.emoji} ${shop.name}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = KebabColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("★", color = KebabColors.starYellow, fontSize = 14.sp)
                            Text(
                                " ${"%.1f".format(shop.rating)} (${shop.reviews})",
                                fontSize = 13.sp,
                                color = KebabColors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = shop.address,
                            fontSize = 12.sp,
                            color = KebabColors.textMuted,
                            maxLines = 1
                        )

                        val uLat = userLocation?.latitude ?: 0.0
                        val uLon = userLocation?.longitude ?: 0.0
                        if (uLat != 0.0) {
                            Text(
                                text = shop.distanceFrom(uLat, uLon).formatDistance(),
                                fontSize = 12.sp,
                                color = KebabColors.accentOrange
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = shop.statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (shop.isOpenNow) KebabColors.openGreen else KebabColors.closedRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            Icons.Filled.ChevronRight,
                            null,
                            tint = KebabColors.textMuted
                        )
                    }
                }
            }
        }
    }
}
