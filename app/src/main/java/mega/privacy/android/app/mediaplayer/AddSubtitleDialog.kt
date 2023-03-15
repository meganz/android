package mega.privacy.android.app.mediaplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import timber.log.Timber

/**
 * Adding subtitle dialog
 *
 * @param matchedItemName the matched subtitle file name
 * @param onAutoMatch the callback for auto match subtitle file
 * @param onToSelectSubtitle the callback for to select subtitle page
 */
@Composable
fun AddSubtitleDialog(
    matchedItemName: String? = null,
    onAutoMatch: () -> Unit,
    onToSelectSubtitle: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier.wrapContentWidth(),
            shape = RoundedCornerShape(5.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(color = colorResource(id = R.color.grey_800))
            ) {
                Text(
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 20.dp,
                        bottom = 20.dp
                    ),
                    text = stringResource(id = R.string.media_player_video_enable_subtitle_dialog_title),
                    color = Color.White,
                    fontSize = 20.sp
                )
                Divider(
                    color = colorResource(id = R.color.grey_300_alpha_026),
                    thickness = 1.dp
                )
                matchedItemName?.let {
                    OptionText(
                        text = stringResource(id = R.string.media_player_video_enable_subtitle_dialog_option_off),
                        onClick = onDismissRequest
                    )
                    OptionText(
                        text = it,
                        color = colorResource(id = R.color.teal_200),
                        onClick = onAutoMatch
                    )
                } ?: OptionText(
                    text = stringResource(id = R.string.media_player_video_enable_subtitle_dialog_option_off),
                    color = colorResource(id = R.color.teal_200),
                    onClick = onDismissRequest
                )
                OptionText(
                    text = stringResource(id = R.string.media_player_video_enable_subtitle_dialog_option_add_subtitle),
                    onClick = onToSelectSubtitle
                )
            }
        }
    }
}

/**
 * The option text
 *
 * @param text option text
 * @param text color
 * @param onClick callback for option clicked
 */
@Composable
private fun OptionText(text: String, color: Color = Color.White, onClick: () -> Unit) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 14.dp
            )
            .clickable {
                onClick()
            },
        text = text,
        color = color,
        fontSize = 16.sp
    )
}

@Preview
@Composable
private fun PreviewAddSubtitleDialogWithMatchedName() {
    AddSubtitleDialog(
        matchedItemName = "Text.srt",
        onAutoMatch = {
            Timber.d("onAutoMatch")
        },
        onToSelectSubtitle = {
            Timber.d("onToSelectSubtitle")
        }) {}
}

@Preview
@Composable
private fun PreviewAddSubtitleDialogWithoutMatchedName() {
    AddSubtitleDialog(
        onAutoMatch = { },
        onToSelectSubtitle = { }) {}
}