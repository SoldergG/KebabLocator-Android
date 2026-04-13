package com.kebablocator.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.MobileAds
import com.kebablocator.android.ui.screens.MainScreen
import com.kebablocator.android.ui.theme.KebabColors
import com.kebablocator.android.ui.theme.KebabLocatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AdMob
        MobileAds.initialize(this) {}

        setContent {
            KebabLocatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = KebabColors.bgPrimary
                ) {
                    MainScreen()
                }
            }
        }
    }
}
