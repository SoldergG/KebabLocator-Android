package com.kebablocator.android.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kebablocator.android.models.KebabShop
import com.kebablocator.android.ui.theme.KebabColors
import com.kebablocator.android.viewmodels.FavoritesViewModel
import com.kebablocator.android.viewmodels.LocationViewModel

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    data object Home : BottomNavItem("home", "Home", Icons.Filled.Home)
    data object Explore : BottomNavItem("explore", "Explore", Icons.Filled.Search)
    data object MapTab : BottomNavItem("map", "Map", Icons.Filled.Map)
    data object Saved : BottomNavItem("saved", "Saved", Icons.Filled.Bookmark)
    data object Location : BottomNavItem("location", "Location", Icons.Filled.LocationOn)
}

private val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Explore,
    BottomNavItem.MapTab,
    BottomNavItem.Saved,
    BottomNavItem.Location
)

@Composable
fun MainScreen(
    locationViewModel: LocationViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Shared selected shop state for navigation
    var selectedShop by remember { mutableStateOf<KebabShop?>(null) }

    val liveShops by locationViewModel.liveShops.collectAsState()
    val allSupabaseShops by favoritesViewModel.allShops.collectAsState()
    val userLocation by locationViewModel.userLocation.collectAsState()

    fun findShop(shopId: String): KebabShop? {
        return selectedShop?.takeIf { it.id == shopId }
            ?: liveShops.find { it.id == shopId }
            ?: allSupabaseShops.find { it.id == shopId }
    }

    fun navigateToShop(shop: KebabShop) {
        selectedShop = shop
        navController.navigate("shop_detail/${shop.id}")
    }

    // Initialize location viewmodel (FavoritesViewModel auto-initializes in init{})
    LaunchedEffect(Unit) {
        locationViewModel.initialize(context)
    }

    Scaffold(
        containerColor = KebabColors.bgPrimary,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar(
                containerColor = KebabColors.bgElevated,
                contentColor = KebabColors.textSecondary,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(text = item.label)
                        },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = KebabColors.accentOrange,
                            selectedTextColor = KebabColors.accentOrange,
                            unselectedIconColor = KebabColors.textMuted,
                            unselectedTextColor = KebabColors.textMuted,
                            indicatorColor = KebabColors.accentOrange.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    locationVM = locationViewModel,
                    favoritesVM = favoritesViewModel,
                    onShopClick = { shop -> navigateToShop(shop) },
                    onAddShopClick = { navController.navigate("add_kebab") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable(BottomNavItem.Explore.route) {
                ExploreScreen(
                    locationVM = locationViewModel,
                    favoritesVM = favoritesViewModel,
                    onShopClick = { shop -> navigateToShop(shop) }
                )
            }
            composable(BottomNavItem.MapTab.route) {
                MapScreen(
                    locationVM = locationViewModel,
                    favoritesVM = favoritesViewModel,
                    onShopClick = { shop -> navigateToShop(shop) }
                )
            }
            composable(BottomNavItem.Saved.route) {
                FavoritesScreen(
                    locationVM = locationViewModel,
                    favoritesVM = favoritesViewModel,
                    onShopClick = { shop -> navigateToShop(shop) }
                )
            }
            composable(BottomNavItem.Location.route) {
                LocationScreen(locationVM = locationViewModel)
            }
            composable("shop_detail/{shopId}") { backStackEntry ->
                val shopId = backStackEntry.arguments?.getString("shopId") ?: return@composable
                val shop = findShop(shopId) ?: return@composable
                ShopDetailScreen(
                    shop = shop,
                    favoritesVM = favoritesViewModel,
                    userLat = userLocation?.latitude ?: 0.0,
                    userLon = userLocation?.longitude ?: 0.0,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("add_kebab") {
                AddKebabScreen(
                    locationVM = locationViewModel,
                    favoritesVM = favoritesViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    locationVM = locationViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
