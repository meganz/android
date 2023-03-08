package mega.privacy.android.data.mapper.mediaplayer

import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import javax.inject.Inject

/**
 * SubtitleFileInfo mapper impl
 */
internal class SubtitleFileInfoMapperImpl @Inject constructor() : SubtitleFileInfoMapper {

    override fun invoke(name: String, url: String?, path: String?): SubtitleFileInfo =
        SubtitleFileInfo(name = name, url = url, path = path)
}