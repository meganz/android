package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ContainsMeta
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import nz.mega.sdk.MegaChatContainsMeta
import javax.inject.Inject

/**
 * Mapper for converting [MegaChatContainsMeta] to [ContainsMeta]
 */
internal class ContainsMetaMapper @Inject constructor(
    private val richPreviewMapper: RichPreviewMapper,
    private val geolocationMapper: ChatGeolocationMapper,
    private val giphyMapper: GiphyMapper,
) {

    operator fun invoke(megaChatContainsMeta: MegaChatContainsMeta) = ContainsMeta(
        type = megaChatContainsMeta.type.toContainsMetaType(),
        textMessage = megaChatContainsMeta.textMessage.orEmpty(),
        richPreview = megaChatContainsMeta.richPreview?.let { richPreviewMapper(it) },
        geolocation = megaChatContainsMeta.geolocation?.let { geolocationMapper(it) },
        giphy = megaChatContainsMeta.giphy?.let { giphyMapper(it) }
    )

    private fun Int.toContainsMetaType(): ContainsMetaType = when (this) {
        MegaChatContainsMeta.CONTAINS_META_INVALID -> ContainsMetaType.INVALID
        MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW -> ContainsMetaType.RICH_PREVIEW
        MegaChatContainsMeta.CONTAINS_META_GEOLOCATION -> ContainsMetaType.GEOLOCATION
        MegaChatContainsMeta.CONTAINS_META_GIPHY -> ContainsMetaType.GIPHY
        else -> ContainsMetaType.INVALID
    }
}