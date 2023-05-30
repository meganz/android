package mega.privacy.android.feature.devicecenter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Test tag for the Device Center Screen
 */
internal const val TAG_DEVICE_CENTER_SCREEN = "DEVICE_CENTER_SCREEN"

/**
 * A [Composable] that serves as the main View for the Device Center
 */
@Composable
internal fun DeviceCenterScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TAG_DEVICE_CENTER_SCREEN),
    )
}

/**
 * Serves as the Preview for [DeviceCenterScreen]
 */
@CombinedThemePreviews
@Composable
private fun DeviceCenterScreenPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterScreen()
    }
}