package mega.privacy.android.app.mediaplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.VideoSpeedPlaybackItem
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.SpeedSelectedDialogEvent

/**
 * The popup for selecting the playback speed in the video player
 *
 * @param items speed playback items
 * @param isShown the popup whether is shown, true is shown, otherwise is false
 * @param currentPlaybackSpeed the current SpeedPlaybackItem
 * @param onDismissRequest on dismiss request
 * @param onItemClick the function for item clicked
 */
@Composable
fun SpeedSelectedPopup(
    items: List<SpeedPlaybackItem>,
    isShown: Boolean,
    currentPlaybackSpeed: SpeedPlaybackItem,
    onDismissRequest: () -> Unit,
    onItemClick: (item: SpeedPlaybackItem) -> Unit,
) {
    LaunchedEffect(isShown) {
        if (isShown) {
            Analytics.tracker.trackEvent(SpeedSelectedDialogEvent)
        }
    }

    if (isShown) {
        Popup(
            onDismissRequest = onDismissRequest,
            alignment = Alignment.BottomStart,
            properties = PopupProperties()
        ) {
            Column(
                modifier = Modifier
                    .width(110.dp)
                    .background(color = colorResource(id = R.color.white_dark_grey))
                    .verticalScroll(rememberScrollState()),
            ) {
                items.map { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onItemClick(item)
                            }
                    ) {
                        MegaText(
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(25.dp, 10.dp),
                            text = item.text,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textColor = if (item == currentPlaybackSpeed) {
                                TextColor.Brand
                            } else {
                                TextColor.Primary
                            },
                        )
                    }

                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewSpeedSelectedPopup() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SpeedSelectedPopup(
            items = VideoSpeedPlaybackItem.entries,
            isShown = true,
            currentPlaybackSpeed = VideoSpeedPlaybackItem.PlaybackSpeed_1X,
            onDismissRequest = {},
            onItemClick = {}
        )
    }
}

