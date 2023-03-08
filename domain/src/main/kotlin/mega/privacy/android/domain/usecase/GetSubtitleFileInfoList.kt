package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo

/**
 * The use case for get [SubtitleFileInfo] list
 */
fun interface GetSubtitleFileInfoList {

    /**
     * Get [SubtitleFileInfo] list
     *
     * @param searchString search string
     */
    suspend operator fun invoke(searchString: String): List<SubtitleFileInfo>
}