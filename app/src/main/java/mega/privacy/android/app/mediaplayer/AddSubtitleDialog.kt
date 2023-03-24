package mega.privacy.android.app.mediaplayer

import android.content.res.Configuration
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import timber.log.Timber

/**
 * Adding subtitle dialog
 *
 * @param matchedSubtitleFileUpdate the callback for get matched subtitle file
 * @param subtitleFileName the added subtitle file name
 * @param onAddedSubtitleClicked the callback for clicking added subtitle file option
 * @param onAutoMatch the callback for auto match subtitle file
 * @param onToSelectSubtitle the callback for to select subtitle page
 * @param onDismissRequest the callback for to select subtitle page
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddSubtitleDialog(
    matchedSubtitleFileUpdate: suspend () -> SubtitleFileInfo?,
    subtitleFileName: String? = null,
    onAddedSubtitleClicked: () -> Unit,
    onAutoMatch: (SubtitleFileInfo) -> Unit,
    onToSelectSubtitle: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    var subtitleFileInfo by remember {
        mutableStateOf<SubtitleFileInfo?>(null)
    }

    LaunchedEffect(Unit) {
        subtitleFileInfo = matchedSubtitleFileUpdate()
    }

    val addedSubtitleFileName by remember(subtitleFileName) {
        mutableStateOf(subtitleFileName)
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .padding(
                    horizontal =
                    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT)
                        40.dp
                    else
                        200.dp
                ),
            shape = RoundedCornerShape(5.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = colorResource(id = R.color.grey_800)
                    )
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
                OptionText(
                    text = stringResource(id = R.string.media_player_video_enable_subtitle_dialog_option_off),
                    onClick = onDismissRequest,
                    isSelected = addedSubtitleFileName == null && subtitleFileInfo == null
                )
                addedSubtitleFileName?.let {
                    OptionText(
                        text = it,
                        isSelected = true,
                        onClick = onAddedSubtitleClicked
                    )
                }
                subtitleFileInfo?.let {
                    OptionText(
                        text = it.name,
                        isSelected = addedSubtitleFileName == null,
                        onClick = {
                            onAutoMatch(it)
                        }
                    )
                }
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
private fun OptionText(
    modifier: Modifier = Modifier,
    text: String,
    selectedColor: Color = colorResource(id = R.color.teal_200),
    unselectedColor: Color = Color.White,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clickable {
                onClick()
            },
        text = text,
        color = if (isSelected) selectedColor else unselectedColor,
        fontSize = 16.sp
    )
}

@Preview
@Composable
private fun PreviewAddSubtitleDialogWithMatchedName() {
    AddSubtitleDialog(
        matchedSubtitleFileUpdate = { null },
        subtitleFileName = "subtitleFile.srt",
        onAddedSubtitleClicked = {
            Timber.d("addedSubtitleFileName")
        },
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
        matchedSubtitleFileUpdate = { null },
        onAddedSubtitleClicked = {},
        onAutoMatch = { },
        onToSelectSubtitle = { }) {}
}