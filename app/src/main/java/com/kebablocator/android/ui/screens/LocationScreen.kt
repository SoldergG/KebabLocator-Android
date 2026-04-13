package com.kebablocator.android.ui.screens

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kebablocator.android.ui.components.glassCard
import com.kebablocator.android.ui.theme.KebabColors
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.kebablocator.android.viewmodels.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(locationVM: LocationViewModel) {
    val context = LocalContext.current
    val userLocation by locationVM.userLocation.collectAsState()
    val isUsingManual by locationVM.isUsingManualLocation.collectAsState()
    val manualAddress by locationVM.manualAddress.collectAsState()
    val searchRadius by locationVM.searchRadius.collectAsState()

    var addressInput by remember { mutableStateOf("") }
    var currentAddress by remember { mutableStateOf("Getting location...") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            locationVM.startLocationUpdates(context)
        }
    }

    // Reverse geocode current location
    LaunchedEffect(userLocation) {
        userLocation?.let {
            locationVM.reverseGeocode(context, it.latitude, it.longitude) { address ->
                currentAddress = address ?: "Location found"
            }
        }
    }

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
            Text(
                text = "Location",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = KebabColors.textPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // GPS Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.GpsFixed,
                        null,
                        tint = if (userLocation != null && !isUsingManual) KebabColors.openGreen else KebabColors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("GPS Location", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary, fontSize = 16.sp)
                        Text(
                            text = if (!isUsingManual && userLocation != null) currentAddress else "Not active",
                            fontSize = 13.sp,
                            color = KebabColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isUsingManual && userLocation != null) KebabColors.openGreen else KebabColors.accentOrange
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (!isUsingManual && userLocation != null) "GPS Active" else "Activate GPS",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Manual Address Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EditLocation, null, tint = KebabColors.accentOrange, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Manual Address", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    placeholder = { Text("Enter address...", color = KebabColors.textMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = KebabColors.textPrimary,
                        unfocusedTextColor = KebabColors.textPrimary,
                        focusedBorderColor = KebabColors.accentOrange,
                        unfocusedBorderColor = KebabColors.textMuted.copy(alpha = 0.3f),
                        cursorColor = KebabColors.accentOrange
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (addressInput.isNotBlank()) {
                            locationVM.geocodeAddress(context, addressInput) { latLng ->
                                if (latLng != null) {
                                    locationVM.setManualLocation(latLng.latitude, latLng.longitude, addressInput)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KebabColors.accentOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search Address", fontWeight = FontWeight.SemiBold)
                }

                if (isUsingManual && manualAddress.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KebabColors.openGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = KebabColors.openGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(manualAddress, fontSize = 13.sp, color = KebabColors.textPrimary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { locationVM.useGPSLocation() }) {
                        Text("Switch back to GPS", color = KebabColors.accentOrange)
                    }
                }
            }
        }

        // Search Radius Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Radar, null, tint = KebabColors.accentOrange, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Search Radius", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary, fontSize = 16.sp)
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
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(
                                "${radius.toInt()} km",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else KebabColors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

