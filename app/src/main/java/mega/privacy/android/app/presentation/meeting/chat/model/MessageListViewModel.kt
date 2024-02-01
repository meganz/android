package mega.privacy.android.app.presentation.meeting.chat.model

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertFooterItem
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.di.meeting.chat.paging.PagedChatMessageRemoteMediatorFactory
import mega.privacy.android.app.presentation.meeting.chat.mapper.UiChatMessageMapper
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.ChatHeaderMessage
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.usecase.chat.message.paging.GetChatPagingSourceUseCase
import javax.inject.Inject

/**
 * Message list view model
 *
 * @property uiChatMessageMapper
 * @property getChatPagingSourceUseCase
 * @constructor
 *
 * @param remoteMediatorFactory
 * @param savedStateHandle
 */
@OptIn(ExperimentalPagingApi::class)
@HiltViewModel
class MessageListViewModel @Inject constructor(
    private val uiChatMessageMapper: UiChatMessageMapper,
    private val getChatPagingSourceUseCase: GetChatPagingSourceUseCase,
    remoteMediatorFactory: PagedChatMessageRemoteMediatorFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val chatId = savedStateHandle.get<Long?>(Constants.CHAT_ID) ?: -1

    /**
     * Latest message time
     */
    val latestMessageId = mutableLongStateOf(-1L)

    /**
     * Asked enable rich link
     */
    val askedEnableRichLink = mutableStateOf(false)

    private val pagedFlow =
        Pager(
            config = PagingConfig(
                pageSize = 32,
                initialLoadSize = 32,
            ),
            remoteMediator = remoteMediatorFactory.create(
                chatId = chatId,
                coroutineScope = viewModelScope,
            ),
        ) {
            getChatPagingSourceUseCase(chatId)
        }.flow
            .map { pagingData ->
                pagingData.map {
                    uiChatMessageMapper(it)
                }
                    .insertFooterItem(
                    item = ChatHeaderMessage()
                )
            }
            .cachedIn(viewModelScope)


    /**
     * Paged messages
     */
    val pagedMessages: Flow<PagingData<UiChatMessage>> = pagedFlow


    /**
     * Update latest message id
     *
     * @param id
     */
    fun updateLatestMessageId(id: Long) {
        latestMessageId.longValue = id
    }

    /**
     * On asked enable rich link
     *
     */
    fun onAskedEnableRichLink() {
        askedEnableRichLink.value = true
    }
}