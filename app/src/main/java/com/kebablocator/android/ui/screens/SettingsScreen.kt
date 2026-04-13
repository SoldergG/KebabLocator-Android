package com.kebablocator.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kebablocator.android.ui.components.glassCard
import com.kebablocator.android.ui.theme.KebabColors
import com.kebablocator.android.viewmodels.LocationViewModel

@Composable
fun SettingsScreen(
    locationVM: LocationViewModel,
    onBack: () -> Unit
) {
    val searchRadius by locationVM.searchRadius.collectAsState()
    val radiusOptions = listOf(5.0, 10.0, 20.0, 40.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KebabColors.bgPrimary)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = KebabColors.textPrimary)
                }
                Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KebabColors.textPrimary)
            }
        }

        // Search Radius
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Radar, null, tint = KebabColors.accentOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Search Radius", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    radiusOptions.forEach { radius ->
                        val isSelected = searchRadius == radius
                        Button(
                            onClick = { locationVM.setSearchRadius(radius) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) KebabColors.accentOrange else KebabColors.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text(
                                "${radius.toInt()} km",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) androidx.compose.ui.graphics.Color.Black else KebabColors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // App Info
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = KebabColors.accentOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("App Info", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                InfoItem("Version", "1.0")
                InfoItem("Platform", "Android")
                InfoItem("Build", "Jetpack Compose")
            }
        }

        // Data Sources
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Storage, null, tint = KebabColors.accentOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Data Sources", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                val sources = listOf(
                    "Google Places" to "Restaurant & venue data",
                    "Yelp" to "Reviews & business details",
                    "OpenStreetMap" to "Community-sourced locations",
                    "Foursquare" to "Venue verification",
                    "Supabase" to "User submissions & storage"
                )

                sources.forEach { (name, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(name, color = KebabColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(desc, color = KebabColors.textMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = KebabColors.textSecondary, fontSize = 14.sp)
        Text(value, color = KebabColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
