package mega.privacy.android.app.mediaplayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.mobile.analytics.event.SpeedOption0_5XPressedEvent
import mega.privacy.mobile.analytics.event.SpeedOption1_5XPressedEvent
import mega.privacy.mobile.analytics.event.SpeedOption2XPressedEvent
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
                    .background(color = colorResource(id = R.color.dark_grey)),
            ) {
                items.map { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (item) {
                                    SpeedPlaybackItem.PLAYBACK_SPEED_0_5_X -> SpeedOption0_5XPressedEvent
                                    SpeedPlaybackItem.PLAYBACK_SPEED_1_5_X -> SpeedOption1_5XPressedEvent
                                    SpeedPlaybackItem.PLAYBACK_SPEED_2_X -> SpeedOption2XPressedEvent
                                    else -> null
                                }?.let { eventIdentifier ->
                                    Analytics.tracker.trackEvent(eventIdentifier)
                                }
                                onItemClick(item)
                            }
                    ) {
                        Image(
                            painter = painterResource(
                                id = item.iconId
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(25.dp, 10.dp),
                            colorFilter = ColorFilter.tint(
                                if (item == currentPlaybackSpeed) {
                                    colorResource(id = R.color.teal_300)
                                } else {
                                    Color.White
                                }
                            )
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
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SpeedSelectedPopup(
            items = VideoPlayerFragment.speedPlaybackList,
            isShown = true,
            currentPlaybackSpeed = SpeedPlaybackItem.PLAYBACK_SPEED_1_X,
            onDismissRequest = {},
            onItemClick = {}
        )
    }
}

