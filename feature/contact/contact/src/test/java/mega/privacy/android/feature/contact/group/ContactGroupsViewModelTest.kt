package mega.privacy.android.feature.contact.group

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.group.ContactGroup
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.contact.group.GetContactGroupsUseCase
import mega.privacy.android.feature.contact.group.mapper.ContactGroupItemMapper
import mega.privacy.android.feature.contact.group.model.ContactGroupItem
import mega.privacy.android.feature.contact.group.model.ContactGroupUiState
import mega.privacy.android.feature.contact.group.model.ContactGroupUiState.Companion.INVALID_GROUP_CHAT_ID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ContactGroupsViewModelTest {

    private lateinit var underTest: ContactGroupsViewModel

    private val getContactGroupsUseCase = mock<GetContactGroupsUseCase>()
    private val createGroupChatRoomUseCase = mock<CreateGroupChatRoomUseCase>()
    private val contactGroupItemMapper = mock<ContactGroupItemMapper>()

    @BeforeEach
    fun setUp() {
        contactGroupItemMapper.stub {
            on { invoke(any()) } doAnswer { invocation ->
                val group = invocation.getArgument<ContactGroup>(0)
                item(chatId = group.chatId, name = group.title)
            }
        }
        underTest = ContactGroupsViewModel(
            createGroupChatRoomUseCase = createGroupChatRoomUseCase,
            getContactGroupsUseCase = getContactGroupsUseCase,
            contactGroupItemMapper = contactGroupItemMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getContactGroupsUseCase,
            createGroupChatRoomUseCase,
            contactGroupItemMapper,
        )
    }

    @Test
    fun `test that initial state is Loading`() = runTest {
        getContactGroupsUseCase.stub { on { invoke() } doReturn emptyList() }

        assertThat(underTest.uiState.value).isEqualTo(ContactGroupUiState.Loading)
    }

    @Test
    fun `test that state is Data with groups when use case succeeds`() = runTest {
        val groups = listOf(group(chatId = 1L, title = "Alpha"))
        getContactGroupsUseCase.stub { on { invoke() } doReturn groups }

        underTest.uiState.test {
            val actual = awaitDataState()
            assertThat(actual.groups).containsExactly(item(chatId = 1L, name = "Alpha"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that state stays Loading when use case throws`() = runTest {
        getContactGroupsUseCase.stub { on { invoke() } doThrow RuntimeException("error") }

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(ContactGroupUiState.Loading)
            expectNoEvents()
        }
    }

    @Test
    fun `test that groupChatCreated event is initially consumed`() = runTest {
        getContactGroupsUseCase.stub { on { invoke() } doReturn emptyList() }

        underTest.uiState.test {
            val actual = awaitDataState()
            assertThat(actual.groupChatCreated).isInstanceOf(StateEventWithContentConsumed::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that setQuery filters groups by title case insensitively`() = runTest {
        val groups = listOf(
            group(chatId = 1L, title = "Alpha"),
            group(chatId = 2L, title = "Beta"),
        )
        getContactGroupsUseCase.stub { on { invoke() } doReturn groups }

        underTest.uiState.test {
            awaitDataState()
            underTest.setQuery("alp")
            val actual = awaitDataState()
            assertThat(actual.groups).containsExactly(item(chatId = 1L, name = "Alpha"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that setQuery with blank query returns all groups`() = runTest {
        val groups = listOf(
            group(chatId = 1L, title = "Alpha"),
            group(chatId = 2L, title = "Beta"),
        )
        getContactGroupsUseCase.stub { on { invoke() } doReturn groups }

        underTest.setQuery("  ")
        underTest.uiState.test {
            val actual = awaitDataState()
            assertThat(actual.groups).containsExactly(
                item(chatId = 1L, name = "Alpha"),
                item(chatId = 2L, name = "Beta"),
            ).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that createGroupChat invokes use case with given arguments`() = runTest {
        getContactGroupsUseCase.stub { on { invoke() } doReturn emptyList() }
        val emails = arrayListOf("a@mega.co.nz", "b@mega.co.nz")
        whenever(
            createGroupChatRoomUseCase(
                emails = any(),
                title = anyOrNull(),
                isEkr = any(),
                addParticipants = any(),
                chatLink = any()
            )
        ) doReturn 123L

        underTest.uiState.test {
            awaitDataState()
            underTest.createGroupChat(emails, "Title", allowAddParticipants = true)
            awaitDataState()
            cancelAndIgnoreRemainingEvents()
        }

        verify(createGroupChatRoomUseCase).invoke(
            emails = emails,
            title = "Title",
            isEkr = false,
            addParticipants = true,
            chatLink = false,
        )
    }

    @Test
    fun `test that groupChatCreated is triggered with chat id when createGroupChat succeeds`() =
        runTest {
            getContactGroupsUseCase.stub { on { invoke() } doReturn emptyList() }
            whenever(
                createGroupChatRoomUseCase(any(), anyOrNull(), any(), any(), any())
            ) doReturn 123L

            underTest.uiState.test {
                awaitDataState()
                underTest.createGroupChat(arrayListOf("a@mega.co.nz"), "Title", true)
                val actual = awaitDataState()
                assertThat(actual.groupChatCreated)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((actual.groupChatCreated as StateEventWithContentTriggered).content)
                    .isEqualTo(123L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that groupChatCreated is triggered with invalid id when createGroupChat fails`() =
        runTest {
            getContactGroupsUseCase.stub { on { invoke() } doReturn emptyList() }
            whenever(
                createGroupChatRoomUseCase(any(), anyOrNull(), any(), any(), any())
            ) doThrow RuntimeException("error")

            underTest.uiState.test {
                awaitDataState()
                underTest.createGroupChat(arrayListOf("a@mega.co.nz"), "Title", true)
                val actual = awaitDataState()
                assertThat(actual.groupChatCreated)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((actual.groupChatCreated as StateEventWithContentTriggered).content)
                    .isEqualTo(INVALID_GROUP_CHAT_ID)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that onGroupChatCreatedConsumed resets the groupChatCreated event`() = runTest {
        getContactGroupsUseCase.stub { on { invoke() } doReturn emptyList() }
        whenever(
            createGroupChatRoomUseCase(any(), anyOrNull(), any(), any(), any())
        ) doReturn 123L

        underTest.uiState.test {
            awaitDataState()
            underTest.createGroupChat(arrayListOf("a@mega.co.nz"), "Title", true)
            val triggered = awaitDataState()
            assertThat(triggered.groupChatCreated)
                .isInstanceOf(StateEventWithContentTriggered::class.java)

            underTest.onGroupChatCreatedConsumed()
            val consumed = awaitDataState()
            assertThat(consumed.groupChatCreated)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun group(chatId: Long, title: String) = ContactGroup(
        chatId = chatId,
        title = title,
        avatar = emptyList(),
        isPublic = true,
    )

    private fun item(chatId: Long, name: String) = ContactGroupItem(
        chatId = chatId,
        name = name,
        avatarData = emptyList(),
        isPrivate = false,
    )

    private suspend fun ReceiveTurbine<ContactGroupUiState>.awaitDataState(): ContactGroupUiState.Data {
        var item = awaitItem()
        while (item !is ContactGroupUiState.Data) {
            item = awaitItem()
        }
        return item
    }
}
