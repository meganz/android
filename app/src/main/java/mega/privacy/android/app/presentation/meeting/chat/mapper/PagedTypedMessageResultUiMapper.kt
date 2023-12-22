package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.paging.PagingLoadResult
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Paged typed message result ui mapper
 *
 * @property uiChatMessageMapper
 */
class PagedTypedMessageResultUiMapper @Inject constructor(
    private val uiChatMessageMapper: UiChatMessageMapper,
) {
    /**
     * Invoke
     *
     * @param pagingLoadResult
     * @param typedMessages
     * @return mapped message list
     */
    operator fun invoke(
        pagingLoadResult: PagingLoadResult?,
        typedMessages: List<TypedMessage>,
    ) = typedMessages.mapIndexed { index, current ->
        val previous = typedMessages.getOrNull(index - 1)
        uiChatMessageMapper(
            message = current,
            showAvatar = shouldShowAvatar(
                current = current,
                nextUserHandle = getNextMessageUserHandle(
                    typedMessages = typedMessages,
                    index = index,
                    pagingLoadResult = pagingLoadResult
                )
            ),
            showTime = shouldShowTime(
                typedMessage = current,
                previous = previous,
            ),
            showDate = previous?.let { shouldShowDate(current, it) } ?: true
        )
    }

    private fun getNextMessageUserHandle(
        typedMessages: List<TypedMessage>,
        index: Int,
        pagingLoadResult: PagingLoadResult?,
    ) = typedMessages.getOrNull(index + 1)?.userHandle ?: pagingLoadResult?.nextMessageUserHandle

    private fun shouldShowAvatar(
        current: TypedMessage,
        nextUserHandle: Long?,
    ) = !current.isMine && !current.hasSameSender(nextUserHandle)

    private fun shouldShowTime(
        typedMessage: TypedMessage,
        previous: TypedMessage?,
    ) = !typedMessage.hasSameSender(previous?.userHandle) || typedMessage.time.minus(
        previous?.time ?: 0
    ) > TimeUnit.MINUTES.toSeconds(3)

    private fun TypedMessage.hasSameSender(
        other: Long?,
    ) = userHandle == other

    private fun shouldShowDate(
        current: TypedMessage,
        previous: TypedMessage,
    ) = previous.getDate() != current.getDate()

    private fun TypedMessage.getDate() =
        Calendar.getInstance().apply {
            timeInMillis = TimeUnit.SECONDS.toMillis(this@getDate.time)
        }.get(Calendar.DATE)
}