package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.Giphy
import nz.mega.sdk.MegaChatGiphy
import javax.inject.Inject

/**
 * Mapper for converting [MegaChatGiphy] into [Giphy]
 */
internal class GiphyMapper @Inject constructor() {

    operator fun invoke(megaChatGiphy: MegaChatGiphy) = Giphy(
        mp4Src = megaChatGiphy.mp4Src,
        webpSrc = megaChatGiphy.webpSrc,
        title = megaChatGiphy.title,
        mp4Size = megaChatGiphy.mp4Size,
        webpSize = megaChatGiphy.webpSize,
        width = megaChatGiphy.width,
        height = megaChatGiphy.height
    )
}