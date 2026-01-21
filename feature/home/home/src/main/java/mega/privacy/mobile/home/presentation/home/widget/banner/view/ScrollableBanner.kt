package mega.privacy.mobile.home.presentation.home.widget.banner.view

import android.annotation.SuppressLint
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.banner.HomeBanner
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.privacy.android.domain.entity.banner.PromotionalBanner as DomainPromoBanner

/**
 * Scrollable banner component that displays multiple PromoBanner instances horizontally
 *
 * @param banners List of domain PromoBanner entities to display
 * @param onDismiss Callback when a banner is dismissed, receives banner ID
 * @param onClick Callback when a banner is clicked, receives banner URL
 * @param modifier Modifier for the composable
 */
@Composable
fun ScrollableBanner(
    banners: List<DomainPromoBanner>,
    onDismiss: (Int, String) -> Unit,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (banners.isNotEmpty()) {

        val listState = rememberLazyListState()
        val cardWidth = rememberBannerCardWidth(bannerCount = banners.size)

        LazyRow(
            modifier = modifier,
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            horizontalArrangement = Arrangement.spacedBy(BANNER_SPACING),
            contentPadding = PaddingValues(horizontal = BANNER_HORIZONTAL_PADDING),
        ) {
            items(
                items = banners,
                key = { it.id }
            ) { promoBanner ->
                HomeBanner(
                    modifier = Modifier.width(cardWidth),
                    backgroundImageUrl = promoBanner.backgroundImage.takeIf { it.isNotEmpty() },
                    imageUrl = promoBanner.image.takeIf { it.isNotEmpty() },
                    title = promoBanner.title,
                    buttonText = promoBanner.buttonText,
                    showDismissButton = true,
                    onClick = { onClick(promoBanner.url) },
                    onDismissClick = { onDismiss(promoBanner.id, promoBanner.url) },
                )
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun rememberBannerCardWidth(bannerCount: Int): Dp {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val deviceType = LocalDeviceType.current
    val isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE

    val cardWidth = remember(bannerCount, screenWidth, deviceType, isLandscape) {
        calculateCardWidth(
            bannerCount = bannerCount,
            screenWidth = screenWidth,
            deviceType = deviceType,
            isLandscape = isLandscape
        )
    }
    return cardWidth
}

private fun calculateCardWidth(
    bannerCount: Int,
    screenWidth: Dp,
    deviceType: DeviceType,
    isLandscape: Boolean,
): Dp {
    return when {
        // Tablet Landscape: Fit all banners if possible, with a minimum width
        deviceType == DeviceType.Tablet && isLandscape -> {
            val totalHorizontalPadding = BANNER_HORIZONTAL_PADDING * 2
            val totalSpacing = BANNER_SPACING * (bannerCount - 1).coerceAtLeast(0)
            val availableWidth = screenWidth - totalHorizontalPadding - totalSpacing
            (availableWidth / bannerCount.coerceAtLeast(1)).coerceAtLeast(200.dp)
        }

        // Tablet Portrait: Standard width
        deviceType == DeviceType.Tablet -> 360.dp

        // Phone Landscape: Show max 2 items to avoid excessive stretching
        isLandscape -> {
            val displayCount = if (bannerCount > 1) 2 else 1
            val totalHorizontalPadding = BANNER_HORIZONTAL_PADDING * 2
            val totalSpacing = if (displayCount > 1) BANNER_SPACING else 0.dp
            val availableWidth = screenWidth - totalHorizontalPadding - totalSpacing
            availableWidth / displayCount
        }

        // Phone Portrait: "Peeking" effect to indicate
        else -> screenWidth - 64.dp
    }
}

private val BANNER_SPACING = 12.dp
private val BANNER_HORIZONTAL_PADDING = 16.dp
