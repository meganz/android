package mega.privacy.android.app.mediaplayer.model

import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo

/**
 * The entity for subtitle displayed
 *
 * @property selected the item whether is selected, true is selected, otherwise is false.
 * @property subtitleFileInfo [SubtitleFileInfo]
 */
data class SubtitleFileInfoItem(
    val selected: Boolean = false,
    val subtitleFileInfo: SubtitleFileInfo
)
