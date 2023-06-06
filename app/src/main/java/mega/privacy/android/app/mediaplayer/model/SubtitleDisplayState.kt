package mega.privacy.android.app.mediaplayer.model

import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo

/**
 * The state for subtitle display
 *
 * @property isSubtitleShown true is show subtitle, otherwise is false
 * @property isSubtitleDialogShown true is show add subtitle dialog, otherwise is false
 * @property isAddSubtitle true is add subtitle, otherwise is show subtitle
 * @property subtitleFileInfo current added subtitle file
 */
data class SubtitleDisplayState(
    val isSubtitleShown: Boolean = false,
    val isSubtitleDialogShown: Boolean = false,
    val isAddSubtitle: Boolean = false,
    val subtitleFileInfo: SubtitleFileInfo? = null,
)