package mega.privacy.android.app.di.meeting.chat.paging

import dagger.assisted.AssistedFactory
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.meeting.chat.model.paging.PagedChatMessageRemoteMediator

/**
 * Paged chat message remote mediator factory
 */
@AssistedFactory
interface PagedChatMessageRemoteMediatorFactory {
    /**
     * Create
     *
     * @param chatId
     * @param coroutineScope
     * @return PagedChatMessageRemoteMediator
     */
    fun create(
        chatId: Long,
        coroutineScope: CoroutineScope,
    ): PagedChatMessageRemoteMediator
}