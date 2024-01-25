package mega.privacy.android.app.presentation.meeting.chat.model.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.usecase.chat.message.paging.ChatHasMoreMessagesUseCase
import mega.privacy.android.domain.usecase.chat.message.paging.ClearChatMessagesUseCase
import mega.privacy.android.domain.usecase.chat.message.paging.FetchMessagePageUseCase
import mega.privacy.android.domain.usecase.chat.message.paging.SaveChatMessagesUseCase
import timber.log.Timber

/**
 * Paged chat message remote mediator
 *
 * @property chatHasMoreMessagesUseCase
 * @property fetchMessages
 * @property saveMessages
 * @property clearChatMessagesUseCase
 * @property chatId
 * @property coroutineScope
 */
@OptIn(ExperimentalPagingApi::class)
class PagedChatMessageRemoteMediator @AssistedInject constructor(
    private val chatHasMoreMessagesUseCase: ChatHasMoreMessagesUseCase,
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
        try {
            Timber.d("Paging mediator load: loadType : $loadType")
            val noMoreMessageAvailable = noMoreMessageAvailable(loadType)
            if (noMoreMessageAvailable) return MediatorResult.Success(
                endOfPaginationReached = true
            )

            if (loadType == LoadType.REFRESH) clearChatMessagesUseCase(chatId)

            val response = fetchMessages(chatId, coroutineScope)
            Timber.d("Paging mediator load: fetch messages response : $response")
            saveMessages(response)
            return MediatorResult.Success(endOfPaginationReached = false)
        } catch (e: Exception) {
            Timber.e(e, "Paging mediator load: error")
            return MediatorResult.Error(e)
        }
    }

    private suspend fun noMoreMessageAvailable(loadType: LoadType) =
        loadType == LoadType.APPEND || !chatHasMoreMessagesUseCase(chatId)

}