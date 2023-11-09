package mega.privacy.android.feature.devicecenter.ui.lists.loading

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.legacy.core.ui.controls.modifier.skeletonEffect
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Test Tag for the Device Center Loading Item
 */
internal const val DEVICE_CENTER_LOADING_ITEM = "device_center_loading_item:row_loading_item"

/**
 * A Composable that is displayed in the Initial Loading Screen
 */
@Composable
internal fun DeviceCenterLoadingItem() {
    Row(
        modifier = Modifier
            .testTag(DEVICE_CENTER_LOADING_ITEM)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(40.dp)
                .skeletonEffect()
        )
        Column(
            modifier = Modifier.padding(start = 12.dp).fillMaxWidth()
        ) {
            Spacer(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(100.dp))
                    .height(12.dp)
                    .width(120.dp)
                    .skeletonEffect()
            )
            Spacer(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(shape = RoundedCornerShape(100.dp))
                    .height(12.dp)
                    .width(183.dp)
                    .skeletonEffect()
            )
        }
    }
}

/**
 * A Preview Composable that displays the Device Center Loading Item
 */
@CombinedThemePreviews
@Composable
private fun PreviewDeviceCenterLoadingItem() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterLoadingItem()
    }
}