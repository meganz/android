package mega.privacy.android.app.presentation.meeting.chat.model.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.presentation.meeting.chat.mapper.PagedTypedMessageResultUiMapper
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.usecase.chat.message.FetchMessagePageUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase
import timber.log.Timber

/**
 * Chat message paging source
 *
 * @property chatId
 * @property loadMessages
 * @property fetchMessages
 * @property messageFlow
 * @property scope
 * @property pagedTypedMessageResultUiMapper
 */
class ChatMessagePagingSource(
    private val chatId: Long,
    private val loadMessages: LoadMessagesUseCase,
    private val fetchMessages: FetchMessagePageUseCase,
    private val messageFlow: Flow<ChatMessage?>,
    private val scope: CoroutineScope,
    private val pagedTypedMessageResultUiMapper: PagedTypedMessageResultUiMapper,
) : PagingSource<PagingLoadResult, UiChatMessage>() {

    override fun getRefreshKey(state: PagingState<PagingLoadResult, UiChatMessage>): PagingLoadResult? {
        val previousKey = state.closestPageToPosition(0)?.prevKey
        Timber.d("Paging: getRefreshKey previous key: $previousKey")

        return previousKey
    }

    override suspend fun load(params: LoadParams<PagingLoadResult>): LoadResult<PagingLoadResult, UiChatMessage> {
        val pagingLoadResult = params.key
        Timber.d("Paging load called with load status: ${pagingLoadResult?.loadStatus}")

        if (pagingLoadResult?.loadStatus == ChatHistoryLoadStatus.NONE) return LoadResult.Page(
            data = emptyList(),
            prevKey = null,
            nextKey = null,
        )

        val typedMessages = scope.async {
            Timber.d("Paging fetch messages called with chat id: $chatId")
            val messageResponse = runCatching {
                fetchMessages(messageFlow)
            }.onFailure { exception ->
                Timber.e(exception, "Paging fetchMessages failed with an exception")
            }.getOrDefault(emptyList())
            Timber.d("Paging Fetch message returned with first message ${messageResponse.firstOrNull()}")
            return@async messageResponse
        }

        val status = scope.async {
            Timber.d("Paging Load messages called with chatId $chatId")
            val returnedStatus = loadMessages(chatId)
            Timber.d("Paging load status response is: $returnedStatus")
            return@async returnedStatus
        }

        val messages = typedMessages.await()
        val historyLoadStatus = status.await()

        val prevKey =
            PagingLoadResult(
                loadStatus = historyLoadStatus,
                nextMessageUserHandle = messages.firstOrNull()?.userHandle,
                nexMessageIsMine = messages.firstOrNull()?.isMine,
            )

        return LoadResult.Page(
            data = pagedTypedMessageResultUiMapper(
                pagingLoadResult = pagingLoadResult,
                typedMessages = messages,
            ),
            prevKey = prevKey,
            nextKey = null,
        )
    }

    override val keyReuseSupported: Boolean
        get() = true
}
