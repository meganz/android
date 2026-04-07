package mega.privacy.android.app.mediaplayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.model.RevampVideoOptionItem
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Overflow menu for the Revamp video player. Uses [RevampVideoOptionItem]; legacy [VideoOptionPopup] is unchanged.
 *
 * @param items Options to show (typically [RevampVideoOptionItem] entries in display order)
 * @param isShown Whether the popup is visible
 * @param onDismissRequest Called when the popup should dismiss
 * @param onItemClick Called when an option is selected
 */
@Composable
fun VideoOptionRevampPopup(
    items: List<RevampVideoOptionItem>,
    isShown: Boolean,
    onDismissRequest: () -> Unit,
    onItemClick: (item: RevampVideoOptionItem) -> Unit,
) {
    if (isShown) {
        Popup(
            onDismissRequest = onDismissRequest,
            alignment = Alignment.BottomStart,
            properties = PopupProperties()
        ) {
            Column(
                modifier = Modifier
                    .background(color = colorResource(id = R.color.dark_grey))
                    .padding(horizontal = 5.dp, vertical = 15.dp)
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .clickable {
                                onItemClick(item)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberVectorPainter(item.icon),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 20.dp),
                            colorFilter = ColorFilter.tint(color = Color.White)
                        )

                        Text(
                            modifier = Modifier
                                .align(alignment = Alignment.CenterVertically)
                                .padding(10.dp, 0.dp, 40.dp, 0.dp),
                            text = stringResource(id = item.optionTitleId),
                            fontSize = 16.sp,
                            color = colorResource(id = R.color.white)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewVideoOptionRevampPopup() {
    val videoOptions = listOf(
        RevampVideoOptionItem.Snapshot,
        RevampVideoOptionItem.Subtitle,
        RevampVideoOptionItem.Playlist,
        RevampVideoOptionItem.Lock,
    )
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        VideoOptionRevampPopup(
            items = videoOptions,
            isShown = true,
            onDismissRequest = {},
            onItemClick = {}
        )
    }
}
