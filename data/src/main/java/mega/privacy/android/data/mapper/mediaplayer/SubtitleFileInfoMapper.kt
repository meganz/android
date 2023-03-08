package mega.privacy.android.data.mapper.mediaplayer

import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo

/**
 * The mapper class for converting the data entity to [SubtitleFileInfo]
 */
internal fun interface SubtitleFileInfoMapper {

    /**
     *Invocation function
     *
     * @param name file name
     * @param url file url
     * @param path file path
     */
    operator fun invoke(name: String, url: String?, path: String?): SubtitleFileInfo
}