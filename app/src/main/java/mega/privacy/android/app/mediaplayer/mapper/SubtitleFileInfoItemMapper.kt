package mega.privacy.android.app.mediaplayer.mapper

import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import javax.inject.Inject

/**
 * The mapper class for converting the data entity to [SubtitleFileInfoItem]
 */
class SubtitleFileInfoItemMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param isSelected the item whether is selected, true is selected, otherwise is false
     * @param subtitleFileInfo [SubtitleFileInfo]
     */
    operator fun invoke(isSelected: Boolean, subtitleFileInfo: SubtitleFileInfo) =
        SubtitleFileInfoItem(selected = isSelected, subtitleFileInfo = subtitleFileInfo)
}