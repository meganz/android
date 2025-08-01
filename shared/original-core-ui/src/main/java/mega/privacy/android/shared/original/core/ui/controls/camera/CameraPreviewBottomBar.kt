package mega.privacy.android.shared.original.core.ui.controls.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalThemeForPreviews

/**
 * Camera preview bottom bar
 *
 * @param modifier
 * @param onSendVideo
 */
@Composable
fun CameraPreviewBottomBar(
    buttonText: String,
    modifier: Modifier = Modifier,
    onSendVideo: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(color = DSTokens.colors.background.pageBackground)
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(172.dp),
        contentAlignment = Alignment.Center,
    ) {
        RaisedDefaultMegaButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TEST_TAG_CAMERA_PREVIEW_BOTTOM_BAR_BUTTON),
            text = buttonText,
            onClick = onSendVideo,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPreviewBottomBarPreview() {
    OriginalThemeForPreviews {
        CameraPreviewBottomBar(
            onSendVideo = {},
            buttonText = "Send Video"
        )
    }
}

internal const val TEST_TAG_CAMERA_PREVIEW_BOTTOM_BAR_BUTTON = "camera_preview_bottom_bar:button"