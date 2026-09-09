package mega.privacy.android.feature.chat.list.archived

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase.ChatRoomType
import mega.privacy.android.feature.chat.list.archived.model.ArchivedChatsUiState
import mega.privacy.android.feature.chat.list.formatter.ChatLastMessageFormatter
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTimestampMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomUiItemMapper
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class ArchivedChatsViewModelTest {

    private lateinit var underTest: ArchivedChatsViewModel

    private val getChatsUseCase = mock<GetChatsUseCase>()
    private val chatLastMessageFormatter = mock<ChatLastMessageFormatter>()
    private val chatRoomTimestampMapper = mock<ChatRoomTimestampMapper>()
    private val chatRoomUiItemMapper = mock<ChatRoomUiItemMapper>()

    private val archivedRoomItem = ChatRoomItem.IndividualChatRoomItem(
        chatId = 1L,
        title = "Archived chat",
    )
    private val archivedUiItem = chatRoomUiItem(chatId = 1L, title = "Archived chat")

    @BeforeEach
    fun setUp() {
        underTest = ArchivedChatsViewModel(
            getChatsUseCase = getChatsUseCase,
            chatLastMessageFormatter = chatLastMessageFormatter,
            chatRoomTimestampMapper = chatRoomTimestampMapper,
            chatRoomUiItemMapper = chatRoomUiItemMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getChatsUseCase,
            chatLastMessageFormatter,
            chatRoomTimestampMapper,
            chatRoomUiItemMapper,
        )
    }

    @Test
    fun `test that initial state is Loading`() = runTest {
        stubArchivedChats()

        assertThat(underTest.uiState.value).isEqualTo(ArchivedChatsUiState.Loading)
    }

    @Test
    fun `test that uiState emits Data with mapped archived chats when they are loaded`() = runTest {
        stubArchivedChats()

        underTest.uiState.test {
            val actual = awaitDataState()

            assertThat(actual.items).containsExactly(archivedUiItem)
        }
    }

    @Test
    fun `test that uiState emits Empty when there are no archived chats`() = runTest {
        stubArchivedChats(archived = emptyList())

        underTest.uiState.test {
            var item = awaitItem()
            while (item is ArchivedChatsUiState.Loading) {
                item = awaitItem()
            }
            assertThat(item).isEqualTo(ArchivedChatsUiState.Empty)
        }
    }

    private fun stubArchivedChats(archived: List<ChatRoomItem> = listOf(archivedRoomItem)) {
        getChatsUseCase.stub {
            on { invoke(eq(ChatRoomType.ARCHIVED_CHATS), any(), any(), any(), any()) } doReturn flow {
                emit(archived)
                awaitCancellation()
            }
        }
        chatRoomUiItemMapper.stub {
            on { invoke(archivedRoomItem) } doReturn archivedUiItem
        }
    }

    private fun chatRoomUiItem(
        chatId: Long,
        title: String,
    ) = ChatRoomUiItem(
        chatId = chatId,
        title = title,
        lastMessage = null,
        lastTimestampFormatted = null,
        header = null,
        pendingMeeting = null,
        showRecurringIcon = false,
        unreadCount = 0,
        isMuted = false,
        highlight = false,
        isNoteToSelf = false,
        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Peer(
            placeholderText = null,
            filePath = null,
            color = null,
        ),
        status = ContactItemStatus.Unknown,
        hasOngoingCall = false,
    )

    private suspend fun ReceiveTurbine<ArchivedChatsUiState>.awaitDataState(): ArchivedChatsUiState.Data {
        var item = awaitItem()
        while (item !is ArchivedChatsUiState.Data) {
            item = awaitItem()
        }
        return item
    }
}
