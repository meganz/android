package test.mega.privacy.android.app.presentation.startconversation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.startconversation.StartConversationViewModel
import mega.privacy.android.core.ui.model.SearchWidgetState
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.AddNewContacts
import mega.privacy.android.domain.usecase.ApplyContactUpdates
import mega.privacy.android.domain.usecase.GetContactDataUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.MonitorContactRequestUpdates
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatPresenceLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.random.Random

@ExperimentalCoroutinesApi
class StartConversationViewModelTest {
    private lateinit var underTest: StartConversationViewModel

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val savedStateHandle = SavedStateHandle(mapOf())
    private val emptyContactData = ContactData(null, null, null)
    private val testContact = ContactItem(
        handle = Random.nextLong(),
        email = "email@mega.nz",
        contactData = emptyContactData,
        defaultAvatarColor = "0asf80",
        visibility = UserVisibility.Visible,
        timestamp = Random.nextLong(),
        areCredentialsVerified = false,
        status = UserStatus.Online,
    )
    private val testContactList = mutableListOf<ContactItem>().apply {
        for (i in 0 until 10) {
            add(
                ContactItem(
                    handle = Random.nextLong(),
                    email = "email$i@mega.nz",
                    contactData = emptyContactData,
                    defaultAvatarColor = "0asf80",
                    visibility = UserVisibility.Visible,
                    timestamp = Random.nextLong(),
                    areCredentialsVerified = false,
                    status = UserStatus.Online,
                )
            )
        }
    }

    private val getVisibleContactsUseCase = mock<GetVisibleContactsUseCase> {
        onBlocking { invoke() }.thenReturn(testContactList)
    }

    private val getContactDataUseCase = mock<GetContactDataUseCase> {
        onBlocking { invoke(mock()) }.thenReturn(mock())
    }

    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase> {
        on { invoke() }.thenReturn(MutableStateFlow(true))
    }

    private val invalidHandle = -1L
    private val chatHandle = Random.nextLong()

    private val startConversationUseCase = mock<StartConversationUseCase> {
        onBlocking { invoke(eq(false), anyOrNull()) }.thenReturn(chatHandle)
    }

    private val monitorContactUpdates = mock<MonitorContactUpdates> {
        on { invoke() }.thenReturn(emptyFlow())
    }

    private val applyContactUpdates = mock<ApplyContactUpdates> {
        onBlocking { invoke(eq(testContactList), any()) }.thenReturn(testContactList)
    }

    private val monitorChatPresenceLastGreenUpdatesUseCase =
        mock<MonitorChatPresenceLastGreenUpdatesUseCase> {
            on { invoke() }.thenReturn(emptyFlow())
        }

    private val monitorChatOnlineStatusUseCase = mock<MonitorChatOnlineStatusUseCase> {
        on { invoke() }.thenReturn(emptyFlow())
    }

    private val monitorContactRequestUpdates = mock<MonitorContactRequestUpdates> {
        on { invoke() }.thenReturn(emptyFlow())
    }

    private val addNewContacts = mock<AddNewContacts> {
        onBlocking { invoke(testContactList, mock()) }.thenReturn(testContactList)
    }

    private val scheduler = TestCoroutineScheduler()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        underTest = StartConversationViewModel(
            getVisibleContactsUseCase = getVisibleContactsUseCase,
            getContactDataUseCase = getContactDataUseCase,
            startConversationUseCase = startConversationUseCase,
            monitorContactUpdates = monitorContactUpdates,
            applyContactUpdates = applyContactUpdates,
            monitorChatPresenceLastGreenUpdatesUseCase = monitorChatPresenceLastGreenUpdatesUseCase,
            monitorChatOnlineStatusUseCase = monitorChatOnlineStatusUseCase,
            monitorContactRequestUpdates = monitorContactRequestUpdates,
            addNewContacts = addNewContacts,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            savedStateHandle = savedStateHandle,
            requestLastGreen = mock(),
            createGroupChatRoomUseCase = mock(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.contactItemList).isEmpty()
            assertThat(initial.emptyViewVisible).isTrue()
            assertThat(initial.searchAvailable).isFalse()
            assertThat(initial.searchWidgetState).isEqualTo(SearchWidgetState.COLLAPSED)
            assertThat(initial.typedSearch).isEmpty()
            assertThat(initial.filteredContactList).isNull()
            assertThat(initial.buttonsVisible).isTrue()
            assertThat(initial.fromChat).isFalse()
        }
    }

    @Test
    fun `test that saved state values are returned`() = runTest {
        val typedSearch = "Typed search"

        savedStateHandle.set(underTest.searchExpandedKey, SearchWidgetState.EXPANDED)
        savedStateHandle.set(underTest.typedSearchKey, typedSearch)
        savedStateHandle.set(underTest.fromChatKey, true)

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
    fun `test that empty view visibility is updated if the contact list is updated and not empty`() =
        runTest {
            underTest.state.map { it.emptyViewVisible }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isTrue()
                    assertThat(awaitItem()).isFalse()
                }
        }

    @Test
    fun `test that search is available if the contact list is updated and not empty`() = runTest {
        underTest.state.map { it.searchAvailable }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isFalse()
                assertThat(awaitItem()).isTrue()
            }
    }

    @Test
    fun `test that searchExpanded is updated if the search view is expanded`() = runTest {
        underTest.state.map { it.searchWidgetState }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isEqualTo(SearchWidgetState.COLLAPSED)
                underTest.updateSearchWidgetState(SearchWidgetState.EXPANDED)
                assertThat(awaitItem()).isEqualTo(SearchWidgetState.EXPANDED)
            }
    }

    @Test
    fun `test that buttons visibility is updated if the search view is expanded`() = runTest {
        underTest.state.map { it.buttonsVisible }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isTrue()
                underTest.updateSearchWidgetState(SearchWidgetState.EXPANDED)
                assertThat(awaitItem()).isFalse()
            }
    }

    @Test
    fun `test that searchExpanded is updated if the search view is expanded and then collapsed`() =
        runTest {
            underTest.state.map { it.searchWidgetState }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(SearchWidgetState.COLLAPSED)
                    underTest.updateSearchWidgetState(SearchWidgetState.EXPANDED)
                    assertThat(awaitItem()).isEqualTo(SearchWidgetState.EXPANDED)
                    underTest.updateSearchWidgetState(SearchWidgetState.COLLAPSED)
                    assertThat(awaitItem()).isEqualTo(SearchWidgetState.COLLAPSED)
                }
        }

    @Test
    fun `test that buttons visibility is updated if the search view is expanded and then collapsed`() =
        runTest {
            underTest.state.map { it.buttonsVisible }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isTrue()
                    underTest.updateSearchWidgetState(SearchWidgetState.EXPANDED)
                    assertThat(awaitItem()).isFalse()
                    underTest.updateSearchWidgetState(SearchWidgetState.COLLAPSED)
                    assertThat(awaitItem()).isTrue()
                }
        }

    @Test
    fun `test that typedSearch is updated if new typedSearch is provided`() = runTest {
        underTest.state.map { it.typedSearch }.distinctUntilChanged()
            .test {
                val newTypedText = "New typed search"

                assertThat(awaitItem()).isEmpty()
                underTest.setTypedSearch(newTypedText)
                assertThat(awaitItem()).isEqualTo(newTypedText)
            }
    }

    @Test
    fun `test that filtered contacts exist if there is a typed search`() = runTest {
        underTest.state.map { it.filteredContactList }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isNull()
                underTest.setTypedSearch("Typed search")
                assertThat(awaitItem()).isNotNull()
            }
    }

    @Test
    fun `test that filtered contacts do not exist if the typed search is removed`() = runTest {
        underTest.state.map { it.filteredContactList }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isNull()
                underTest.setTypedSearch("Typed search")
                assertThat(awaitItem()).isNotNull()
                underTest.setTypedSearch("")
                assertThat(awaitItem()).isNull()
            }
    }

    @Test
    fun `test that connection error is returned if attempting to start a conversation and no internet available`() =
        runTest {
            whenever(monitorConnectivityUseCase()).thenReturn(MutableStateFlow(true))

            underTest.state.map { it.error }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isNull()
                    underTest.onContactTap(testContact)
                    assertThat(awaitItem()).isEqualTo(R.string.check_internet_connection_error)
                }
        }

    @Test
    fun `test that an invalid handle is returned if start conversation finish with an error`() =
        runTest {
            whenever(
                startConversationUseCase(
                    isGroup = anyOrNull(),
                    userHandles = anyOrNull()
                )
            ).thenAnswer { throw Throwable("Complete with error") }
            scheduler.advanceUntilIdle()

            underTest.state.map { it.result }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isNull()
                    underTest.onContactTap(testContact)
                    assertThat(awaitItem()).isEqualTo(invalidHandle)
                }
        }

    @Test
    fun `test that an error is returned if start conversation finish with an error`() =
        runTest {
            whenever(
                startConversationUseCase(
                    isGroup = anyOrNull(),
                    userHandles = anyOrNull()
                )
            ).thenAnswer { throw Throwable("Complete with error") }
            scheduler.advanceUntilIdle()

            underTest.state.map { it.error }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isNull()
                    underTest.onContactTap(testContact)
                    assertThat(awaitItem()).isEqualTo(R.string.general_text_error)
                }
        }

    @Test
    fun `test that conversation handle is returned if start conversation finishes without an error`() =
        runTest {
            scheduler.advanceUntilIdle()

            underTest.state.map { it.result }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isNull()
                    underTest.onContactTap(testContact)
                    assertThat(awaitItem()).isEqualTo(chatHandle)
                }
        }
}