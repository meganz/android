package mega.privacy.android.app.presentation.videoplayer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videoplayer.model.SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
import mega.privacy.android.app.presentation.videoplayer.model.SUBTITLE_SELECTED_STATE_MATCHED_ITEM
import mega.privacy.android.app.presentation.videoplayer.model.SUBTITLE_SELECTED_STATE_OFF
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Rows for the add-subtitle bottom sheet. Trailing UI: radio for all except [AddFromCloud] (chevron).
 */
internal sealed class VideoPlayerSubtitleSheetAction {
    abstract val testTag: String

    data object Off : VideoPlayerSubtitleSheetAction() {
        override val testTag = VIDEO_PLAYER_SUBTITLE_SHEET_ROW_OFF_TEST_TAG
    }

    data class AddedSubtitle(val fileName: String) : VideoPlayerSubtitleSheetAction() {
        override val testTag = VIDEO_PLAYER_SUBTITLE_SHEET_ROW_ADDED_TEST_TAG
    }

    data class AutoMatched(val info: SubtitleFileInfo) : VideoPlayerSubtitleSheetAction() {
        override val testTag = VIDEO_PLAYER_SUBTITLE_SHEET_ROW_MATCHED_TEST_TAG
    }

    data object AddFromCloud : VideoPlayerSubtitleSheetAction() {
        override val testTag = VIDEO_PLAYER_SUBTITLE_SHEET_ROW_CLOUD_TEST_TAG
    }
}

internal fun buildSubtitleSheetRows(
    subtitleFileName: String?,
    matchedSubtitle: SubtitleFileInfo?,
): List<VideoPlayerSubtitleSheetAction> = buildList {
    add(VideoPlayerSubtitleSheetAction.Off)
    subtitleFileName?.let { add(VideoPlayerSubtitleSheetAction.AddedSubtitle(it)) }
    matchedSubtitle?.let { add(VideoPlayerSubtitleSheetAction.AutoMatched(it)) }
    add(VideoPlayerSubtitleSheetAction.AddFromCloud)
}

internal fun handleSubtitleSheetAction(
    action: VideoPlayerSubtitleSheetAction,
    onOffClicked: () -> Unit,
    onAddedSubtitleClicked: () -> Unit,
    onAutoMatch: (SubtitleFileInfo) -> Unit,
    onToSelectSubtitle: () -> Unit,
) {
    when (action) {
        VideoPlayerSubtitleSheetAction.Off -> onOffClicked()
        is VideoPlayerSubtitleSheetAction.AddedSubtitle -> onAddedSubtitleClicked()
        is VideoPlayerSubtitleSheetAction.AutoMatched -> onAutoMatch(action.info)
        VideoPlayerSubtitleSheetAction.AddFromCloud -> onToSelectSubtitle()
    }
}

internal fun VideoPlayerSubtitleSheetAction.isRadioSelected(selectOptionState: Int): Boolean =
    when (this) {
        VideoPlayerSubtitleSheetAction.Off ->
            selectOptionState == SUBTITLE_SELECTED_STATE_OFF

        is VideoPlayerSubtitleSheetAction.AddedSubtitle ->
            selectOptionState == SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM

        is VideoPlayerSubtitleSheetAction.AutoMatched ->
            selectOptionState == SUBTITLE_SELECTED_STATE_MATCHED_ITEM

        VideoPlayerSubtitleSheetAction.AddFromCloud -> false
    }

@Composable
internal fun VideoPlayerSubtitleSheetAction.sheetTitle(): String =
    when (this) {
        VideoPlayerSubtitleSheetAction.Off ->
            stringResource(id = R.string.media_player_video_enable_subtitle_dialog_option_off)

        is VideoPlayerSubtitleSheetAction.AddedSubtitle -> fileName

        is VideoPlayerSubtitleSheetAction.AutoMatched -> info.name

        VideoPlayerSubtitleSheetAction.AddFromCloud ->
            stringResource(id = sharedR.string.video_player_subtitle_bottom_sheet_add_subtitle_option)
    }

const val VIDEO_PLAYER_SUBTITLE_SHEET_ROW_OFF_TEST_TAG = "video_player_subtitle_sheet:row_off"

const val VIDEO_PLAYER_SUBTITLE_SHEET_ROW_ADDED_TEST_TAG =
    "video_player_subtitle_sheet:row_added_subtitle"

const val VIDEO_PLAYER_SUBTITLE_SHEET_ROW_MATCHED_TEST_TAG =
    "video_player_subtitle_sheet:row_auto_matched_subtitle"

const val VIDEO_PLAYER_SUBTITLE_SHEET_ROW_CLOUD_TEST_TAG =
    "video_player_subtitle_sheet:row_add_from_cloud"
