package mega.privacy.android.feature.devicecenter.ui.lists.loading

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

/**
 * Test Tag for the Device Center Loading Screen
 */
internal const val DEVICE_CENTER_LOADING_SCREEN =
    "device_center_loading_screen:lazy_column_initial_loading"

/**
 * A Composable that displays the initial Loading Screen when the User opens the Device Center
 */
@Composable
internal fun DeviceCenterLoadingScreen() {
    LazyColumn(
        modifier = Modifier.testTag(DEVICE_CENTER_LOADING_SCREEN),
        userScrollEnabled = false,
        content = {
            items(count = 20) {
                DeviceCenterLoadingItem()
            }
        }
    )
}

/**
 * A Preview Composable that displays the Loading Screen
 */
@CombinedThemePreviews
@Composable
private fun PreviewDeviceCenterLoadingScreen() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterLoadingScreen()
    }
}