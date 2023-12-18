package test.mega.privacy.android.app.presentation.startconversation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.startconversation.StartConversationViewModel
import mega.privacy.android.app.presentation.startconversation.model.StartConversationAction
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.AddNewContacts
import mega.privacy.android.domain.usecase.ApplyContactUpdates
import mega.privacy.android.domain.usecase.GetContactDataUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.MonitorContactRequestUpdates
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatPresenceLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import test.mega.privacy.android.app.InstantExecutorExtension

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartConversationViewModelTest {

    private lateinit var underTest: StartConversationViewModel

    private var savedStateHandle = SavedStateHandle(mapOf())
    private val emptyContactData = ContactData(null, null, null)
    private val testContact = ContactItem(
        handle = 123L,
        email = "email@mega.nz",
        contactData = emptyContactData,
        defaultAvatarColor = "0asf80",
        visibility = UserVisibility.Visible,
        timestamp = 12346L,
        areCredentialsVerified = false,
        status = UserChatStatus.Online,
    )
    private val testContactList = buildList {
        for (i in 0 until 10) {
            add(getContact(i.toLong()))
        }
    }

    private fun getContact(handle: Long) = ContactItem(
        handle = handle,
        email = "email$handle@mega.nz",
        contactData = emptyContactData,
        defaultAvatarColor = "0asf80",
        visibility = UserVisibility.Visible,
        timestamp = 654321L,
        areCredentialsVerified = false,
        status = UserChatStatus.Online,
    )

    private val invalidHandle = -1L
    private val chatHandle = 123L

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
    private val monitorContactRequestUpdates = mock<MonitorContactRequestUpdates>()
    private val addNewContacts = mock<AddNewContacts>()
    private val requestUserLastGreenUseCase = mock<RequestUserLastGreenUseCase>()
    private val createGroupChatRoomUseCase = mock<CreateGroupChatRoomUseCase>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getVisibleContactsUseCase,
            getContactDataUseCase,
            startConversationUseCase,
            applyContactUpdates,
            addNewContacts,
            requestUserLastGreenUseCase,
            createGroupChatRoomUseCase
        )
        savedStateHandle = SavedStateHandle(mapOf())
        wheneverBlocking { getVisibleContactsUseCase() }.thenReturn(emptyList())
        wheneverBlocking { getContactDataUseCase(any()) }.thenReturn(mock())
        wheneverBlocking { monitorConnectivityUseCase() }.thenReturn(connectivityFlow)
        wheneverBlocking { monitorContactUpdates() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorChatPresenceLastGreenUpdatesUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorChatOnlineStatusUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorContactRequestUpdates() }.thenReturn(emptyFlow())
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
            monitorContactRequestUpdates = monitorContactRequestUpdates,
            addNewContacts = addNewContacts,
            requestUserLastGreenUseCase = requestUserLastGreenUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
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
        }.test(200) {
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
        }
    }

    @Test
    fun `test that filtered contacts do not exist if the typed search is removed`() = runTest {
        whenever(getVisibleContactsUseCase()).thenReturn(testContactList)
        initTestClass()

        testScheduler.advanceUntilIdle()
        underTest.state.map { it.filteredContactList }.drop(1).test {
            underTest.setTypedSearch("email1")
            assertThat(awaitItem()).isEqualTo(listOf(getContact(1)))
            underTest.setTypedSearch("")
            assertThat(awaitItem()).isNull()
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
}