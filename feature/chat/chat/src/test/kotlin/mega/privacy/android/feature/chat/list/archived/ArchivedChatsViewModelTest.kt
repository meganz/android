package mega.privacy.android.feature.chat.list.archived

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase.ChatRoomType
import mega.privacy.android.feature.chat.list.archived.model.ArchivedChatsUiState
import mega.privacy.android.feature.chat.list.formatter.ChatLastMessageFormatter
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTimestampMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomActionPluralMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTitleMapper
import mega.privacy.android.feature.chat.list.mapper.ChatRoomUiItemMapper
import mega.privacy.android.feature.chat.list.menu.ArchiveChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_UNARCHIVE_TAG
import mega.privacy.android.feature.chat.list.menu.ChatRoomActions
import mega.privacy.android.feature.chat.list.menu.ChatRoomMenuItem
import mega.privacy.android.feature.chat.list.menu.UnarchiveChatRoomMenuItem
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import kotlinx.coroutines.test.advanceUntilIdle

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class ArchivedChatsViewModelTest {

    private lateinit var underTest: ArchivedChatsViewModel

    private val getChatsUseCase = mock<GetChatsUseCase>()
    private val chatLastMessageFormatter = mock<ChatLastMessageFormatter>()
    private val chatRoomTimestampMapper = mock<ChatRoomTimestampMapper>()
    private val chatRoomUiItemMapper = mock<ChatRoomUiItemMapper>()
    private val archiveChatUseCase = mock<ArchiveChatUseCase>()
    private val chatRoomTitleMapper = mock<ChatRoomTitleMapper>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    private val chatRoomMenuItems: Map<Int, ChatRoomMenuItem> = mapOf(
        2 to ArchiveChatRoomMenuItem(archiveChatUseCase, chatRoomTitleMapper),
        3 to UnarchiveChatRoomMenuItem(archiveChatUseCase, chatRoomTitleMapper),
    )

    private val chatRoomActionPluralMapper = mock<ChatRoomActionPluralMapper>()

    private val chatRoomActions = ChatRoomActions(
        chatRoomMenuItems = chatRoomMenuItems,
        chatRoomActionPluralMapper = chatRoomActionPluralMapper,
        snackbarEventQueue = snackbarEventQueue,
    )

    private val archivedRoomItem = ChatRoomItem.IndividualChatRoomItem(
        chatId = 1L,
        title = "Archived chat",
        isArchived = true,
    )
    private val archivedUiItem = chatRoomUiItem(chatId = 1L, title = "Archived chat")

    @BeforeEach
    fun setUp() {
        underTest = ArchivedChatsViewModel(
            getChatsUseCase = getChatsUseCase,
            chatLastMessageFormatter = chatLastMessageFormatter,
            chatRoomTimestampMapper = chatRoomTimestampMapper,
            chatRoomUiItemMapper = chatRoomUiItemMapper,
            chatRoomActions = chatRoomActions,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getChatsUseCase,
            chatLastMessageFormatter,
            chatRoomTimestampMapper,
            chatRoomUiItemMapper,
            archiveChatUseCase,
            chatRoomTitleMapper,
            snackbarEventQueue,
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

    @Test
    fun `test that getChatRoomActions offers unarchive for an archived chat`() = runTest {
        stubArchivedChats()

        val actual = underTest.getChatRoomActions(setOf(1L)).map { it.testTag }

        assertThat(actual).containsExactly(CHAT_ROOM_ACTIONS_UNARCHIVE_TAG)
    }

    @Test
    fun `test that getChatRoomActions returns empty for an unknown chat`() = runTest {
        stubArchivedChats()

        assertThat(underTest.getChatRoomActions(setOf(999L))).isEmpty()
    }

    @Test
    fun `test that getChatRoomActions rethrows cancellation instead of resolving against an empty selection when the coroutine reading the chat rooms is cancelled`() =
        runTest {
            lateinit var running: Job
            var actions: List<ChatRoomActionUiItem>? = null
            var thrown: Throwable? = null
            getChatsUseCase.stub {
                on {
                    invoke(eq(ChatRoomType.ARCHIVED_CHATS), any(), any(), any(), any())
                } doAnswer {
                    flow {
                        running.cancel()
                        throw CancellationException("cancelled")
                    }
                }
            }

            running = launch {
                thrown = runCatching { actions = underTest.getChatRoomActions(setOf(1L)) }
                    .exceptionOrNull()
            }
            advanceUntilIdle()

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
            assertThat(actions).isNull()
        }

    @Test
    fun `test that onChatRoomActionSelected unarchives the picked chat`() = runTest {
        stubArchivedChats()

        underTest.onChatRoomActionSelected(setOf(1L), CHAT_ROOM_ACTIONS_UNARCHIVE_TAG)
        advanceUntilIdle()

        verify(archiveChatUseCase)(1L, false)
    }
}
