package mega.privacy.android.app.presentation.meeting.chat.model.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.usecase.chat.message.paging.ClearChatMessagesUseCase
import mega.privacy.android.domain.usecase.chat.message.paging.FetchMessagePageUseCase
import mega.privacy.android.domain.usecase.chat.message.paging.SaveChatMessagesUseCase
import timber.log.Timber

/**
 * Paged chat message remote mediator
 *
 * @property fetchMessages
 * @property saveMessages
 * @property clearChatMessagesUseCase
 * @property chatId
 * @property coroutineScope
 */
@OptIn(ExperimentalPagingApi::class)
class PagedChatMessageRemoteMediator @AssistedInject constructor(
    private val fetchMessages: FetchMessagePageUseCase,
    private val saveMessages: SaveChatMessagesUseCase,
    private val clearChatMessagesUseCase: ClearChatMessagesUseCase,
    @Assisted private val chatId: Long,
    @Assisted private val coroutineScope: CoroutineScope,
) : RemoteMediator<Int, TypedMessage>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, TypedMessage>,
    ): MediatorResult {
        return try {
            Timber.d("Paging mediator load: loadType : $loadType")

            if (loadType == LoadType.REFRESH) clearChatMessagesUseCase(chatId)

            val response = fetchMessages(chatId, coroutineScope)
            Timber.d("Paging mediator load: fetch messages response : $response")
            saveMessages(response)
            MediatorResult.Success(endOfPaginationReached = loadType == LoadType.REFRESH || response.loadResponse == ChatHistoryLoadStatus.NONE)
        } catch (e: Exception) {
            Timber.e(e, "Paging mediator load: error")
            MediatorResult.Error(e)
        }
    }

}