package mega.privacy.android.core.ui.controls.camera

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
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Camera preview bottom bar
 *
 * @param modifier
 * @param onSendVideo
 */
@Composable
fun CameraPreviewBottomBar(
    modifier: Modifier = Modifier,
    onSendVideo: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(color = MegaTheme.colors.background.pageBackground)
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(172.dp),
        contentAlignment = Alignment.Center,
    ) {
        RaisedDefaultMegaButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TEST_TAG_CAMERA_PREVIEW_BOTTOM_BAR_BUTTON),
            textId = R.string.context_send,
            onClick = onSendVideo,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPreviewBottomBarPreview() {
    AndroidTheme {
        CameraPreviewBottomBar(onSendVideo = {})
    }
}

internal const val TEST_TAG_CAMERA_PREVIEW_BOTTOM_BAR_BUTTON = "camera_preview_bottom_bar:button"