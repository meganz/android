package mega.privacy.android.data.mapper.chat.paging

import mega.privacy.android.data.database.entity.chat.MetaTypedMessageEntity
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.usecase.chat.message.CreateInvalidMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateTypedMessageUseCase
import javax.inject.Inject

/**
 * Meta typed entity typed message mapper
 *
 * @property createTypedMessageUseCases
 * @property createInvalidMessageUseCase
 */
class MetaTypedEntityTypedMessageMapper @Inject constructor(
    private val createTypedMessageUseCases: Map<@JvmSuppressWildcards ChatMessageType, @JvmSuppressWildcards CreateTypedMessageUseCase>,
    private val createInvalidMessageUseCase: CreateInvalidMessageUseCase,
) {
    /**
     * Invoke
     *
     * @param entity
     * @return
     */
    operator fun invoke(entity: MetaTypedMessageEntity): TypedMessage {
        val request = CreateTypedMessageRequest(
            message = entity.typedMessageEntity,
            isMine = entity.typedMessageEntity.isMine,
            shouldShowAvatar = entity.typedMessageEntity.shouldShowTime,
            shouldShowTime = entity.typedMessageEntity.shouldShowDate,
            shouldShowDate = entity.typedMessageEntity.shouldShowAvatar,
            metaType = getMetaType(entity),
            textMessage = entity.typedMessageEntity.textMessage,
            chatRichPreviewInfo = entity.richPreviewEntity,
            chatGeolocationInfo = entity.geolocationEntity,
            chatGifInfo = entity.giphyEntity,
            nodeList = entity.nodeList,
        )

        return createTypedMessageUseCases[entity.typedMessageEntity.type]?.invoke(request)
            ?: createInvalidMessageUseCase(request)
    }

    private fun getMetaType(entity: MetaTypedMessageEntity) = when {
        entity.richPreviewEntity != null -> ContainsMetaType.RICH_PREVIEW
        entity.geolocationEntity != null -> ContainsMetaType.GEOLOCATION
        entity.giphyEntity != null -> ContainsMetaType.GIPHY
        else -> ContainsMetaType.INVALID
    }

}
