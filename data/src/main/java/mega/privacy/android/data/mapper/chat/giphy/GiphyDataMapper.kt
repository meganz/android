package mega.privacy.android.data.mapper.chat.giphy

import mega.privacy.android.data.model.giphy.GiphyData
import mega.privacy.android.domain.entity.chat.Giphy
import javax.inject.Inject

/**
 * Maps a [GiphyData] to a [Giphy] using its fixed height rendition.
 */
internal class GiphyDataMapper @Inject constructor() {

    operator fun invoke(giphyData: GiphyData): Giphy {
        val attributes = giphyData.images?.fixedHeight
        return Giphy(
            mp4Src = attributes?.mp4,
            webpSrc = attributes?.webp,
            title = giphyData.title,
            mp4Size = attributes?.mp4Size?.toInt() ?: 0,
            webpSize = attributes?.webpSize?.toInt() ?: 0,
            width = attributes?.width ?: 0,
            height = attributes?.height ?: 0,
        )
    }

    operator fun invoke(giphyData: List<GiphyData>): List<Giphy> = giphyData.map { invoke(it) }
}
