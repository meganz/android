package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.RichPreview
import nz.mega.sdk.MegaChatRichPreview
import javax.inject.Inject

/**
 * Mapper for converting [MegaChatRichPreview] into [RichPreview].
 */
internal class RichPreviewMapper @Inject constructor() {

    operator fun invoke(megaChatRichPreview: MegaChatRichPreview) =
        RichPreview(
            title = megaChatRichPreview.title.orEmpty(),
            description = megaChatRichPreview.description.orEmpty(),
            image = megaChatRichPreview.image,
            imageFormat = megaChatRichPreview.imageFormat,
            icon = megaChatRichPreview.icon,
            iconFormat = megaChatRichPreview.iconFormat,
            url = megaChatRichPreview.url,
            domainName = megaChatRichPreview.domainName
        )
}