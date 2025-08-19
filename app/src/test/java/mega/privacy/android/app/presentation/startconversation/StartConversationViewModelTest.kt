package mega.privacy.android.app.presentation.startconversation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.InstantExecutorExtension
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.startconversation.model.StartConversationAction
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.ApplyContactUpdates
import mega.privacy.android.domain.usecase.GetContactDataUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.contact.AddNewContactsUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatPresenceLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExtendWith(InstantExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartConversationViewModelTest {

    private lateinit var underTest: StartConversationViewModel

    private var savedStateHandle = SavedStateHandle(mapOf())
    private val emptyContactData = ContactData(
        fullName = null, alias = null, avatarUri = null,
        userVisibility = UserVisibility.Unknown,
    )
    private val testContact = ContactItem(
        handle = 123L,
        email = "email@mega.io",
        contactData = emptyContactData,
        defaultAvatarColor = "0asf80",
        visibility = UserVisibility.Visible,
        timestamp = 12346L,
        areCredentialsVerified = false,
        status = UserChatStatus.Online,
        chatroomId = null,
    )

    private val testContactList = buildList {
        for (i in 0 until 10) {
            add(getContact(i.toLong()))
        }
    }

    private fun getContact(handle: Long) = ContactItem(
        handle = handle,
        email = "email$handle@mega.io",
        contactData = emptyContactData,
        defaultAvatarColor = "0asf80",
        visibility = UserVisibility.Visible,
        timestamp = 654321L,
        areCredentialsVerified = false,
        status = UserChatStatus.Online,
        chatroomId = null,
    )

    private val invalidHandle = -1L
    private val chatHandle = 123L

    private val noteToSelfChat = mock<ChatRoom> {
        on { chatId } doReturn chatHandle
        on { isNoteToSelf } doReturn true
    }

    private val getVisibleContactsUseCase = mock<GetVisibleContactsUseCase>()
    private val getContactDataUseCase = mock<GetContactDataUseCase>()
    private var connectivityFlow = MutableSharedFlow<Boolean>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val startConversationUseCase = mock<StartConversationUseCase>()
    private val monitorContactUpdates = mock<MonitorContactUpdates>()
    private val applyContactUpdates = mock<ApplyContactUpdates>()
    private val monitorChatPresenceLastGreenUpdatesUseCase =
        mock<MonitorChatPresenceLastGreenUpdatesUseCase>()
    private val monitorChatOnlineStatusUseCase = mock<MonitorChatOnlineStatusUseCase>()
    private val monitorContactRequestUpdatesUseCase = mock<MonitorContactRequestUpdatesUseCase>()
    private val addNewContactsUseCase = mock<AddNewContactsUseCase>()
    private val requestUserLastGreenUseCase = mock<RequestUserLastGreenUseCase>()
    private val createGroupChatRoomUseCase = mock<CreateGroupChatRoomUseCase>()
    private val getNoteToSelfChatUseCase = mock<GetNoteToSelfChatUseCase>()

    @BeforeEach
    fun resetMocks() {
        reset(
            getVisibleContactsUseCase,
            getContactDataUseCase,
            startConversationUseCase,
            applyContactUpdates,
            addNewContactsUseCase,
            requestUserLastGreenUseCase,
            createGroupChatRoomUseCase,
            getNoteToSelfChatUseCase
        )
        savedStateHandle = SavedStateHandle(mapOf())
        wheneverBlocking { getVisibleContactsUseCase() }.thenReturn(emptyList())
        wheneverBlocking { getNoteToSelfChatUseCase() }.thenReturn(mock())
        wheneverBlocking { getContactDataUseCase(any()) }.thenReturn(mock())
        wheneverBlocking { monitorConnectivityUseCase() }.thenReturn(connectivityFlow)
        wheneverBlocking { monitorContactUpdates() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorChatPresenceLastGreenUpdatesUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorChatOnlineStatusUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorContactRequestUpdatesUseCase() }.thenReturn(emptyFlow())
        initTestClass()
    }

    private fun initTestClass() {
        underTest = StartConversationViewModel(
            getVisibleContactsUseCase = getVisibleContactsUseCase,
            getContactDataUseCase = getContactDataUseCase,
            startConversationUseCase = startConversationUseCase,
            createGroupChatRoomUseCase = createGroupChatRoomUseCase,
            monitorContactUpdates = monitorContactUpdates,
            applyContactUpdates = applyContactUpdates,
            monitorChatPresenceLastGreenUpdatesUseCase = monitorChatPresenceLastGreenUpdatesUseCase,
            monitorChatOnlineStatusUseCase = monitorChatOnlineStatusUseCase,
            monitorContactRequestUpdatesUseCase = monitorContactRequestUpdatesUseCase,
            addNewContactsUseCase = addNewContactsUseCase,
            requestUserLastGreenUseCase = requestUserLastGreenUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getNoteToSelfChatUseCase = getNoteToSelfChatUseCase,
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.buttons).containsExactly(*StartConversationAction.entries.toTypedArray())
            assertThat(initial.contactItemList).isEmpty()
            assertThat(initial.emptyViewVisible).isTrue()
            assertThat(initial.searchAvailable).isFalse()
            assertThat(initial.searchWidgetState).isEqualTo(SearchWidgetState.COLLAPSED)
            assertThat(initial.typedSearch).isEmpty()
            assertThat(initial.filteredContactList).isNull()
            assertThat(initial.buttonsVisible).isTrue()
            assertThat(initial.error).isNull()
            assertThat(initial.result).isNull()
            assertThat(initial.fromChat).isFalse()
        }
    }

    @Test
    fun `test that saved state values are returned`() = runTest {
        val typedSearch = "Typed search"

        savedStateHandle[underTest.searchExpandedKey] = SearchWidgetState.EXPANDED
        savedStateHandle[underTest.typedSearchKey] = typedSearch
        savedStateHandle[underTest.fromChatKey] = true

        underTest.state.filter {
            it.searchWidgetState == SearchWidgetState.EXPANDED
                    && it.typedSearch == typedSearch
                    && it.fromChat
        }.test {
            val latest = awaitItem()
            assertThat(latest.searchWidgetState).isEqualTo(SearchWidgetState.EXPANDED)
            assertThat(latest.typedSearch).isEqualTo(typedSearch)
            assertThat(latest.fromChat).isTrue()
        }
    }

    @Test
    fun `test that searchExpanded is updated if the search view is expanded`() = runTest {
        underTest.state.map { it.searchWidgetState }.drop(1).test {
            underTest.updateSearchWidgetState(SearchWidgetState.EXPANDED)
            assertThat(awaitItem()).isEqualTo(SearchWidgetState.EXPANDED)
        }
    }

    @Test
    fun `test that buttons visibility is updated if the search view is expanded`() = runTest {
        underTest.state.map { it.buttonsVisible }.drop(1).test {
            underTest.updateSearchWidgetState(SearchWidgetState.EXPANDED)
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that searchExpanded is updated if the search view is expanded and then collapsed`() =
        runTest {
            underTest.state.map { it.searchWidgetState }.drop(1).test {
                underTest.updateSearchWidgetState(SearchWidgetState.EXPANDED)
                assertThat(awaitItem()).isEqualTo(SearchWidgetState.EXPANDED)
                underTest.updateSearchWidgetState(SearchWidgetState.COLLAPSED)
                assertThat(awaitItem()).isEqualTo(SearchWidgetState.COLLAPSED)
            }
        }

    @Test
    fun `test that buttons visibility is updated if the search view is expanded and then collapsed`() =
        runTest {
            underTest.state.map { it.buttonsVisible }.drop(1).test {
                underTest.updateSearchWidgetState(SearchWidgetState.EXPANDED)
                assertThat(awaitItem()).isFalse()
                underTest.updateSearchWidgetState(SearchWidgetState.COLLAPSED)
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `test that typedSearch is updated if new typedSearch is provided`() = runTest {
        val newTypedText = "New typed search"
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.typedSearch }.drop(1).test {
            underTest.setTypedSearch(newTypedText)
            assertThat(awaitItem()).isEqualTo(newTypedText)
        }
    }

    @Test
    fun `test that filtered contacts exist if there is a typed search`() = runTest {
        whenever(getVisibleContactsUseCase()).thenReturn(testContactList)
        initTestClass()

        underTest.state.map { it.filteredContactList }.drop(1).test {
            underTest.setTypedSearch("email1")
            assertThat(awaitItem()).isEqualTo(listOf(getContact(1)))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that filtered contacts do not exist if the typed search is removed`() = runTest {
        whenever(getVisibleContactsUseCase()).thenReturn(testContactList)
        wheneverBlocking { getContactDataUseCase(any()) }.thenReturn(emptyContactData)
        initTestClass()

        testScheduler.advanceUntilIdle()
        underTest.state.map { it.filteredContactList }.drop(1).test {
            underTest.setTypedSearch("email1")
            assertThat(awaitItem()).isEqualTo(listOf(getContact(1)))
            underTest.setTypedSearch("")
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that connection error is returned if attempting to start a conversation and no internet available`() =
        runTest {
            testScheduler.advanceUntilIdle()
            underTest.state.map { it.error }.drop(1).test {
                connectivityFlow.emit(false)
                underTest.onContactTap(testContact)
                assertThat(awaitItem()).isEqualTo(R.string.check_internet_connection_error)
            }
        }

    @Test
    fun `test that connection error is returned if attempting to get note to self and no internet available`() =
        runTest {
            testScheduler.advanceUntilIdle()
            underTest.state.map { it.error }.drop(1).test {
                connectivityFlow.emit(false)
                underTest.openNoteToSelf()
                assertThat(awaitItem()).isEqualTo(R.string.check_internet_connection_error)
            }
        }

    @Test
    fun `test that an invalid handle is returned if start conversation finish with an error`() =
        runTest {
            whenever(startConversationUseCase(isGroup = anyOrNull(), userHandles = anyOrNull()))
                .thenAnswer { throw Throwable("Complete with error") }

            testScheduler.advanceUntilIdle()
            underTest.state.map { it.result }.drop(1).test {
                connectivityFlow.emit(true)
                underTest.onContactTap(testContact)
                assertThat(awaitItem()).isEqualTo(invalidHandle)
            }
        }

    @Test
    fun `test that an invalid handle is returned if get note to self finish with an error`() =
        runTest {
            whenever(getNoteToSelfChatUseCase())
                .thenAnswer { throw Throwable("Complete with error") }

            testScheduler.advanceUntilIdle()
            underTest.state.map { it.error }.drop(1).test {
                connectivityFlow.emit(true)
                underTest.openNoteToSelf()
                assertThat(awaitItem()).isEqualTo(R.string.general_text_error)
            }
        }

    @Test
    fun `test that an error is returned if start conversation finish with an error`() =
        runTest {
            whenever(startConversationUseCase(isGroup = anyOrNull(), userHandles = anyOrNull()))
                .thenAnswer { throw Throwable("Complete with error") }

            testScheduler.advanceUntilIdle()
            underTest.state.map { it.error }.drop(1).test {
                connectivityFlow.emit(true)
                underTest.onContactTap(testContact)
                assertThat(awaitItem()).isEqualTo(R.string.general_text_error)
            }
        }

    @Test
    fun `test that conversation handle is returned if start conversation finishes without an error`() =
        runTest {
            whenever(startConversationUseCase(isGroup = anyOrNull(), userHandles = anyOrNull()))
                .thenReturn(chatHandle)

            testScheduler.advanceUntilIdle()
            underTest.state.map { it.result }.drop(1).test {
                connectivityFlow.emit(true)
                underTest.onContactTap(testContact)
                assertThat(awaitItem()).isEqualTo(chatHandle)
            }
        }

    @Test
    fun `test that note to self chat is returned if get note to self finishes without an error`() =
        runTest {
            whenever(getNoteToSelfChatUseCase())
                .thenReturn(noteToSelfChat)

            testScheduler.advanceUntilIdle()
            underTest.state.map { it.result }.drop(1).test {
                connectivityFlow.emit(true)
                underTest.openNoteToSelf()
                assertThat(awaitItem()).isEqualTo(chatHandle)
            }
        }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}