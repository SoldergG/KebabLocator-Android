package com.kebablocator.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kebablocator.android.ui.theme.KebabColors
import kotlin.math.sin

// Glass Card Modifier
fun Modifier.glassCard(
    cornerRadius: Dp = 18.dp,
    opacity: Float = 0.08f,
    borderOpacity: Float = 0.15f
): Modifier = this
    .shadow(
        elevation = 12.dp,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = Color.Black.copy(alpha = 0.15f),
        spotColor = Color.Black.copy(alpha = 0.15f)
    )
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = opacity),
                Color.White.copy(alpha = opacity * 0.3f),
                Color.Transparent
            ),
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    )
    .background(KebabColors.bgElevated.copy(alpha = 0.85f))
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderOpacity),
                Color.White.copy(alpha = borderOpacity * 0.3f),
                KebabColors.accentOrange.copy(alpha = borderOpacity * 0.2f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

// Glass Background
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.background(KebabColors.bgPrimary)) {
        // Ambient glow blobs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-200).dp)
                .blur(80.dp)
                .background(
                    KebabColors.accentOrange.copy(alpha = 0.06f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = 150.dp, y = 100.dp)
                .blur(100.dp)
                .background(
                    Color(0xFF3B82F6).copy(alpha = 0.04f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = (-50).dp, y = 300.dp)
                .blur(90.dp)
                .background(
                    Color(0xFF8B5CF6).copy(alpha = 0.03f),
                    CircleShape
                )
        )
        content()
    }
}

// Glass Pill (for tags/badges)
@Composable
fun GlassPill(
    text: String,
    color: Color = KebabColors.accentOrange,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
            .background(
                color.copy(alpha = 0.08f),
                RoundedCornerShape(50)
            )
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.1f))
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

// Glass Stat Card
@Composable
fun GlassStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color = KebabColors.accentOrange,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .glassCard(cornerRadius = 16.dp)
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = KebabColors.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = KebabColors.textMuted,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

// Banner Ad Composable (placeholder - real AdMob integration)
@Composable
fun BannerAd(
    adUnitId: String = "ca-app-pub-3940256099942544/6300978111", // Test ID
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = "Ad",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = KebabColors.textMuted
            )
        }
    }
}
