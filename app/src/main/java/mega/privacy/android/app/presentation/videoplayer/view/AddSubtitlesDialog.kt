package mega.privacy.android.app.presentation.videoplayer.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videoplayer.model.SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
import mega.privacy.android.app.presentation.videoplayer.model.SUBTITLE_SELECTED_STATE_MATCHED_ITEM
import mega.privacy.android.app.presentation.videoplayer.model.SUBTITLE_SELECTED_STATE_OFF
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaRadioButton
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.mobile.analytics.event.AddSubtitleDialogEvent

@Composable
fun AddSubtitlesDialog(
    isShown: Boolean,
    selectOptionState: Int,
    matchedSubtitleFileUpdate: suspend () -> SubtitleFileInfo?,
    onOffClicked: () -> Unit,
    onAddedSubtitleClicked: () -> Unit,
    onAutoMatch: (SubtitleFileInfo) -> Unit,
    onToSelectSubtitle: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleFileName: String? = null,
) {
    var subtitleFileInfo by remember {
        mutableStateOf<SubtitleFileInfo?>(null)
    }

    LaunchedEffect(isShown) {
        if (isShown) {
            subtitleFileInfo = matchedSubtitleFileUpdate()
            Analytics.tracker.trackEvent(AddSubtitleDialogEvent)
        }
    }
    val addedSubtitleFileName by remember(subtitleFileName) {
        mutableStateOf(subtitleFileName)
    }
    if (isShown) {
        MegaAlertDialog(
            text = {
                Column {
                    MegaText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, start = 15.dp)
                            .testTag(ADD_SUBTITLE_DIALOG_TITLE_TEST_TAG),
                        text = stringResource(id = R.string.media_player_video_enable_subtitle_dialog_title),
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.titleLarge
                    )

                    SubtitleOptionWithRadioButton(
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .testTag(ADD_SUBTITLE_DIALOG_OFF_ROW_TEST_TAG),
                        selected = selectOptionState == SUBTITLE_SELECTED_STATE_OFF,
                        optionText = stringResource(id = R.string.media_player_video_enable_subtitle_dialog_option_off),
                        onClick = onOffClicked
                    )

                    addedSubtitleFileName?.let {
                        SubtitleOptionWithRadioButton(
                            modifier = Modifier
                                .testTag(ADD_SUBTITLE_DIALOG_ADDED_SUBTITLE_ROW_TEST_TAG),
                            selected = selectOptionState == SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM,
                            optionText = it,
                            onClick = onAddedSubtitleClicked
                        )
                    }
                    subtitleFileInfo?.let {
                        SubtitleOptionWithRadioButton(
                            modifier = Modifier
                                .testTag(ADD_SUBTITLE_DIALOG_AUTO_MATCHED_SUBTITLE_ROW_TEST_TAG),
                            selected = selectOptionState == SUBTITLE_SELECTED_STATE_MATCHED_ITEM,
                            optionText = it.name,
                            onClick = {
                                onAutoMatch(it)
                            }
                        )
                    }

                    MegaText(
                        modifier = Modifier
                            .padding(top = 20.dp, start = 15.dp)
                            .fillMaxWidth()
                            .clickable { onToSelectSubtitle() }
                            .testTag(ADD_SUBTITLE_DIALOG_NAVIGATE_TO_SELECT_SUBTITLE_TEST_TAG),
                        text = stringResource(id = R.string.media_player_video_enable_subtitle_dialog_option_add_subtitle),
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButtonText = stringResource(R.string.button_cancel),
            cancelButtonText = null,
            onConfirm = onDismissRequest,
            onDismiss = onDismissRequest,
            modifier = modifier,
        )
    }
}

@Composable
private fun SubtitleOptionWithRadioButton(
    selected: Boolean,
    optionText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        MegaRadioButton(
            selected = selected,
            onClick = { onClick() }
        )
        Spacer(modifier = Modifier.width(15.dp))
        MegaText(
            text = optionText,
            textColor = TextColor.Primary,
            style = if (selected)
                MaterialTheme.typography.titleMedium
            else
                MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Test tag for the title of the add subtitle dialog
 */
const val ADD_SUBTITLE_DIALOG_TITLE_TEST_TAG = "add_subtitle_dialog:text_title"

/**
 * Test tag for the off row of the add subtitle dialog
 */
const val ADD_SUBTITLE_DIALOG_OFF_ROW_TEST_TAG = "add_subtitle_dialog:row_off"

/**
 * Test tag for the added subtitle row of the add subtitle dialog
 */
const val ADD_SUBTITLE_DIALOG_ADDED_SUBTITLE_ROW_TEST_TAG = "add_subtitle_dialog:row_added_subtitle"

/**
 * Test tag for the auto matched subtitle text of the add subtitle dialog
 */
const val ADD_SUBTITLE_DIALOG_AUTO_MATCHED_SUBTITLE_ROW_TEST_TAG =
    "add_subtitle_dialog:row_auto_matched_subtitle"

/**
 * Test tag for the navigate to select subtitle option of the add subtitle dialog
 */
const val ADD_SUBTITLE_DIALOG_NAVIGATE_TO_SELECT_SUBTITLE_TEST_TAG =
    "add_subtitle_dialog:text_navigate_to_select_subtitle"
