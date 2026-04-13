package com.kebablocator.android.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kebablocator.android.models.KebabCategory
import com.kebablocator.android.models.SupabaseShopDTO
import com.kebablocator.android.ui.components.glassCard
import com.kebablocator.android.ui.theme.KebabColors
import com.kebablocator.android.viewmodels.FavoritesViewModel
import com.kebablocator.android.viewmodels.LocationViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKebabScreen(
    locationVM: LocationViewModel,
    favoritesVM: FavoritesViewModel,
    onBack: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    // Form state
    var isKebab by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(KebabCategory.DONER) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var useCurrentLocation by remember { mutableStateOf(true) }
    var manualAddress by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var openHour by remember { mutableIntStateOf(11) }
    var closeHour by remember { mutableIntStateOf(23) }
    var hasDelivery by remember { mutableStateOf(true) }
    var hasDineIn by remember { mutableStateOf(true) }
    var hasTakeaway by remember { mutableStateOf(true) }
    var price by remember { mutableStateOf("€€") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val userLocation by locationVM.userLocation.collectAsState()

    val availableTags = listOf(
        "Halal", "Vegan", "Late Night", "24h", "Delivery", "Family",
        "Student Spot", "Premium", "Fast", "Cheap", "Trendy",
        "Hidden Gem", "Authentic", "BBQ", "Charcoal Grill"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KebabColors.bgPrimary)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (currentStep > 0) currentStep-- else onBack()
            }) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = KebabColors.textPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Add a Place", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary, fontSize = 18.sp)
                Text("Step ${currentStep + 1} of $totalSteps", fontSize = 12.sp, color = KebabColors.textMuted)
            }
        }

        // Progress Bar
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / totalSteps },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(4.dp),
            color = KebabColors.accentOrange,
            trackColor = KebabColors.surface,
        )

        // Step Content
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (currentStep) {
                0 -> {
                    // Step 1: Type
                    item {
                        Text("What type of place?", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TypeCard(
                                emoji = "🥙",
                                title = "Kebab Shop",
                                selected = isKebab,
                                onClick = { isKebab = true },
                                modifier = Modifier.weight(1f)
                            )
                            TypeCard(
                                emoji = "🏪",
                                title = "Convenience",
                                selected = !isKebab,
                                onClick = { isKebab = false },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                1 -> {
                    // Step 2: Basic info
                    item {
                        Text("Basic Info", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Shop Name") },
                            colors = fieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Category", fontWeight = FontWeight.SemiBold, color = KebabColors.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KebabCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text("${cat.emoji} ${cat.displayName}", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = KebabColors.accentOrange.copy(alpha = 0.2f),
                                        selectedLabelColor = KebabColors.accentOrange,
                                        containerColor = KebabColors.bgElevated,
                                        labelColor = KebabColors.textSecondary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tags", fontWeight = FontWeight.SemiBold, color = KebabColors.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableTags.forEach { tag ->
                                FilterChip(
                                    selected = tag in selectedTags,
                                    onClick = {
                                        selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                                    },
                                    label = { Text(tag, fontSize = 12.sp) },
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
                }
                2 -> {
                    // Step 3: Location
                    item {
                        Text("Location", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TypeCard(
                                emoji = "📍",
                                title = "Current GPS",
                                selected = useCurrentLocation,
                                onClick = { useCurrentLocation = true },
                                modifier = Modifier.weight(1f)
                            )
                            TypeCard(
                                emoji = "✏️",
                                title = "Manual",
                                selected = !useCurrentLocation,
                                onClick = { useCurrentLocation = false },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!useCurrentLocation) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = manualAddress,
                                onValueChange = { manualAddress = it },
                                label = { Text("Address") },
                                colors = fieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (userLocation != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(KebabColors.openGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, null, tint = KebabColors.openGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("GPS location detected", color = KebabColors.openGreen, fontSize = 13.sp)
                            }
                        }
                    }
                }
                3 -> {
                    // Step 4: Details
                    item {
                        Text("Details", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = phone, onValueChange = { phone = it },
                            label = { Text("Phone") },
                            colors = fieldColors(), shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = website, onValueChange = { website = it },
                            label = { Text("Website") },
                            colors = fieldColors(), shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Hours", fontWeight = FontWeight.SemiBold, color = KebabColors.textPrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Open:", color = KebabColors.textSecondary)
                            Slider(
                                value = openHour.toFloat(),
                                onValueChange = { openHour = it.toInt() },
                                valueRange = 0f..23f,
                                steps = 22,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = KebabColors.accentOrange, activeTrackColor = KebabColors.accentOrange)
                            )
                            Text("${openHour}:00", color = KebabColors.textPrimary, fontSize = 14.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Close:", color = KebabColors.textSecondary)
                            Slider(
                                value = closeHour.toFloat(),
                                onValueChange = { closeHour = it.toInt() },
                                valueRange = 0f..24f,
                                steps = 23,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = KebabColors.accentOrange, activeTrackColor = KebabColors.accentOrange)
                            )
                            Text("${closeHour}:00", color = KebabColors.textPrimary, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Services", fontWeight = FontWeight.SemiBold, color = KebabColors.textPrimary)
                        CheckboxRow("Delivery", hasDelivery) { hasDelivery = it }
                        CheckboxRow("Dine-In", hasDineIn) { hasDineIn = it }
                        CheckboxRow("Takeaway", hasTakeaway) { hasTakeaway = it }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Price", fontWeight = FontWeight.SemiBold, color = KebabColors.textPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("€", "€€", "€€€", "€€€€").forEach { p ->
                                FilterChip(
                                    selected = price == p,
                                    onClick = { price = p },
                                    label = { Text(p) },
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
                }
                4 -> {
                    // Step 5: Review & Submit
                    item {
                        Text("Review & Submit", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = description, onValueChange = { description = it },
                            label = { Text("Description (optional)") },
                            colors = fieldColors(), shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Summary", fontWeight = FontWeight.Bold, color = KebabColors.textPrimary)
                            ReviewRow("Type", if (isKebab) "Kebab Shop" else "Convenience")
                            ReviewRow("Name", name.ifBlank { "—" })
                            ReviewRow("Category", "${category.emoji} ${category.displayName}")
                            ReviewRow("Tags", selectedTags.joinToString(", ").ifBlank { "None" })
                            ReviewRow("Hours", "${openHour}:00 – ${closeHour}:00")
                            ReviewRow("Price", price)
                            ReviewRow("Services", buildList {
                                if (hasDelivery) add("Delivery")
                                if (hasDineIn) add("Dine-In")
                                if (hasTakeaway) add("Takeaway")
                            }.joinToString(", "))
                        }
                    }
                }
            }
        }

        // Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KebabColors.textPrimary)
                ) {
                    Text("Back")
                }
            }

            Button(
                onClick = {
                    if (currentStep < totalSteps - 1) {
                        currentStep++
                    } else {
                        // Submit
                        isSubmitting = true
                        val lat = userLocation?.latitude ?: 0.0
                        val lon = userLocation?.longitude ?: 0.0

                        val dto = SupabaseShopDTO(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            rating = 4.0,
                            reviews = 0,
                            address = if (useCurrentLocation) "GPS Location" else manualAddress,
                            latitude = lat,
                            longitude = lon,
                            tags = selectedTags.toList(),
                            price = price,
                            hours = "${openHour}:00 – ${closeHour}:00",
                            openHour = openHour,
                            closeHour = closeHour,
                            description = description,
                            category = category.displayName,
                            phone = phone.ifBlank { null },
                            website = website.ifBlank { null },
                            popularDishes = emptyList(),
                            hasDelivery = hasDelivery,
                            hasDineIn = hasDineIn,
                            hasTakeaway = hasTakeaway,
                            imageUrl = null,
                            isSponsored = false,
                            isVerified = false,
                            contributorId = null
                        )

                        favoritesVM.submitShop(dto, null) { success ->
                            isSubmitting = false
                            if (success) onBack()
                        }
                    }
                },
                enabled = !isSubmitting && (currentStep != 1 || name.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = KebabColors.accentOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(if (currentStep > 0) 1f else 2f)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = KebabColors.textPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (currentStep < totalSteps - 1) "Next" else "Submit",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeCard(emoji: String, title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) KebabColors.accentOrange.copy(alpha = 0.15f)
                else KebabColors.bgElevated
            )
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        Text(emoji, fontSize = 40.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            title,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) KebabColors.accentOrange else KebabColors.textSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = KebabColors.accentOrange)
        )
        Text(label, color = KebabColors.textPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = KebabColors.textMuted, fontSize = 13.sp)
        Text(value, color = KebabColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = KebabColors.textPrimary,
    unfocusedTextColor = KebabColors.textPrimary,
    focusedLabelColor = KebabColors.accentOrange,
    unfocusedLabelColor = KebabColors.textMuted,
    focusedBorderColor = KebabColors.accentOrange,
    unfocusedBorderColor = KebabColors.textMuted.copy(alpha = 0.3f),
    cursorColor = KebabColors.accentOrange
)
