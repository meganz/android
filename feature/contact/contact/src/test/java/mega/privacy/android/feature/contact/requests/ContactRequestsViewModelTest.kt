package mega.privacy.android.feature.contact.requests

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.domain.entity.contacts.ContactRequestLists
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestsUseCase
import mega.privacy.android.domain.usecase.account.contactrequest.ReplyContactRequestUseCase
import mega.privacy.android.feature.contact.requests.mapper.ContactRequestUiItemMapper
import mega.privacy.android.feature.contact.requests.model.ContactRequestTab
import mega.privacy.android.feature.contact.requests.model.ContactRequestUiItem
import mega.privacy.android.feature.contact.requests.model.ContactRequestsUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactRequestsViewModelTest {

    private lateinit var underTest: ContactRequestsViewModel

    private val monitorContactRequestsUseCase = mock<MonitorContactRequestsUseCase>()
    private val replyContactRequestUseCase = mock<ReplyContactRequestUseCase>()
    private val contactRequestUiItemMapper = ContactRequestUiItemMapper()

    private fun initUnderTest(
        initialTab: ContactRequestTab = ContactRequestTab.Received,
        mapper: ContactRequestUiItemMapper = contactRequestUiItemMapper,
    ) {
        underTest = ContactRequestsViewModel(
            initialTab = initialTab,
            monitorContactRequestsUseCase = monitorContactRequestsUseCase,
            replyContactRequestUseCase = replyContactRequestUseCase,
            contactRequestUiItemMapper = mapper,
        )
    }

    @BeforeEach
    fun setUp() {
        stubRequests(incoming = emptyList(), outgoing = emptyList())
    }

    @AfterEach
    fun tearDown() {
        reset(monitorContactRequestsUseCase, replyContactRequestUseCase)
    }

    @Test
    fun `test that uiState maps incoming requests to received and outgoing requests to sent`() =
        runTest {
            stubRequests(
                incoming = listOf(createRequest(1L, isOutgoing = false)),
                outgoing = listOf(createRequest(2L, isOutgoing = true)),
            )
            initUnderTest()

            underTest.uiState.test {
                val state = awaitItem() as ContactRequestsUiState.Data
                assertThat(state.received.map { it.handle }).containsExactly(1L)
                assertThat(state.sent.map { it.handle }).containsExactly(2L)
                assertThat(state.selectedTab).isEqualTo(ContactRequestTab.Received)
                assertThat(state.query).isNull()
            }
        }

    @Test
    fun `test that uiState selectedTab is seeded from the initial tab`() = runTest {
        initUnderTest(initialTab = ContactRequestTab.Sent)

        underTest.uiState.test {
            val state = awaitItem() as ContactRequestsUiState.Data
            assertThat(state.selectedTab).isEqualTo(ContactRequestTab.Sent)
        }
    }

    @Test
    fun `test that selectTab updates the selected tab in the uiState`() = runTest {
        initUnderTest(initialTab = ContactRequestTab.Received)

        underTest.uiState.test {
            assertThat((awaitItem() as ContactRequestsUiState.Data).selectedTab)
                .isEqualTo(ContactRequestTab.Received)

            underTest.selectTab(ContactRequestTab.Sent)

            assertThat((awaitItem() as ContactRequestsUiState.Data).selectedTab)
                .isEqualTo(ContactRequestTab.Sent)
        }
    }

    @Test
    fun `test that uiState filters received and sent by email ignoring case when setQuery is called`() =
        runTest {
            stubRequests(
                incoming = listOf(
                    createRequest(1L, isOutgoing = false, sourceEmail = "Alice@example.com"),
                    createRequest(2L, isOutgoing = false, sourceEmail = "bob@example.com"),
                ),
                outgoing = listOf(
                    createRequest(3L, isOutgoing = true, targetEmail = "ALICIA@example.com"),
                    createRequest(4L, isOutgoing = true, targetEmail = "carol@example.com"),
                ),
            )
            initUnderTest()

            underTest.uiState.test {
                awaitItem()

                underTest.setQuery("ali")

                val state = awaitItem() as ContactRequestsUiState.Data
                assertThat(state.received.map { it.handle }).containsExactly(1L)
                assertThat(state.sent.map { it.handle }).containsExactly(3L)
                assertThat(state.query).isEqualTo("ali")
            }
        }

    @Test
    fun `test that uiState filters by display name ignoring case when setQuery is called`() =
        runTest {
            val mapper = mock<ContactRequestUiItemMapper> {
                on { invoke(any()) } doAnswer { invocation ->
                    val request = invocation.getArgument<ContactRequest>(0)
                    val mapped = contactRequestUiItemMapper(request)
                    val name = if (request.handle == 1L) "Alice Anderson" else "Bob Brown"
                    mapped.copy(contact = mapped.contact.copy(displayName = name))
                }
            }
            stubRequests(
                incoming = listOf(
                    createRequest(1L, isOutgoing = false, sourceEmail = "one@example.com"),
                    createRequest(2L, isOutgoing = false, sourceEmail = "two@example.com"),
                ),
                outgoing = emptyList(),
            )
            initUnderTest(mapper = mapper)

            underTest.uiState.test {
                awaitItem()

                underTest.setQuery("ANDERSON")

                val state = awaitItem() as ContactRequestsUiState.Data
                assertThat(state.received.map { it.handle }).containsExactly(1L)
            }
        }

    @Test
    fun `test that uiState restores every request when setQuery is called with null`() = runTest {
        stubRequests(
            incoming = listOf(
                createRequest(1L, isOutgoing = false, sourceEmail = "alice@example.com"),
                createRequest(2L, isOutgoing = false, sourceEmail = "bob@example.com"),
            ),
            outgoing = emptyList(),
        )
        initUnderTest()

        underTest.uiState.test {
            awaitItem()
            underTest.setQuery("bob")
            assertThat((awaitItem() as ContactRequestsUiState.Data).received.map { it.handle })
                .containsExactly(2L)

            underTest.setQuery(null)

            val state = awaitItem() as ContactRequestsUiState.Data
            assertThat(state.received.map { it.handle }).containsExactly(1L, 2L)
            assertThat(state.query).isNull()
        }
    }

    @Test
    fun `test that uiState keeps the query when resubscribed after the stop timeout`() = runTest {
        stubRequests(
            incoming = listOf(
                createRequest(1L, isOutgoing = false, sourceEmail = "alice@example.com"),
                createRequest(2L, isOutgoing = false, sourceEmail = "bob@example.com"),
            ),
            outgoing = emptyList(),
        )
        initUnderTest()
        underTest.uiState.test {
            awaitItem()
            underTest.setQuery("bob")
            awaitItem()
        }

        advanceTimeBy(UI_STATE_STOP_TIMEOUT_MS + 1)

        underTest.uiState.test {
            val state = expectMostRecentItem() as ContactRequestsUiState.Data
            assertThat(state.received.map { it.handle }).containsExactly(2L)
            assertThat(state.query).isEqualTo("bob")
        }
    }

    @Test
    fun `test that uiState selects Sent when seeded with Received and only sent requests exist`() =
        runTest {
            stubRequests(
                incoming = emptyList(),
                outgoing = listOf(createRequest(2L, isOutgoing = true)),
            )
            initUnderTest(initialTab = ContactRequestTab.Received)

            underTest.uiState.test {
                val state = awaitItem() as ContactRequestsUiState.Data
                assertThat(state.selectedTab).isEqualTo(ContactRequestTab.Sent)
                expectNoEvents()
            }
        }

    @Test
    fun `test that uiState selects Received when seeded with Sent and only received requests exist`() =
        runTest {
            stubRequests(
                incoming = listOf(createRequest(1L, isOutgoing = false)),
                outgoing = emptyList(),
            )
            initUnderTest(initialTab = ContactRequestTab.Sent)

            underTest.uiState.test {
                val state = awaitItem() as ContactRequestsUiState.Data
                assertThat(state.selectedTab).isEqualTo(ContactRequestTab.Received)
                expectNoEvents()
            }
        }

    @Test
    fun `test that uiState keeps the seeded tab when it has requests`() = runTest {
        stubRequests(
            incoming = listOf(createRequest(1L, isOutgoing = false)),
            outgoing = listOf(createRequest(2L, isOutgoing = true)),
        )
        initUnderTest(initialTab = ContactRequestTab.Sent)

        underTest.uiState.test {
            val state = awaitItem() as ContactRequestsUiState.Data
            assertThat(state.selectedTab).isEqualTo(ContactRequestTab.Sent)
            expectNoEvents()
        }
    }

    @Test
    fun `test that uiState keeps the seeded tab when both tabs are empty`() = runTest {
        initUnderTest(initialTab = ContactRequestTab.Sent)

        underTest.uiState.test {
            val state = awaitItem() as ContactRequestsUiState.Data
            assertThat(state.selectedTab).isEqualTo(ContactRequestTab.Sent)
            expectNoEvents()
        }
    }

    @Test
    fun `test that uiState keeps the selected tab when a later emission empties it`() = runTest {
        val requests = MutableSharedFlow<ContactRequestLists>(replay = 1)
        requests.tryEmit(
            ContactRequestLists(
                incomingContactRequests = listOf(createRequest(1L, isOutgoing = false)),
                outgoingContactRequests = emptyList(),
            )
        )
        whenever(monitorContactRequestsUseCase()).thenReturn(requests)
        initUnderTest(initialTab = ContactRequestTab.Received)

        underTest.uiState.test {
            assertThat((awaitItem() as ContactRequestsUiState.Data).selectedTab)
                .isEqualTo(ContactRequestTab.Received)

            requests.emit(
                ContactRequestLists(
                    incomingContactRequests = emptyList(),
                    outgoingContactRequests = listOf(createRequest(2L, isOutgoing = true)),
                )
            )

            val state = awaitItem() as ContactRequestsUiState.Data
            assertThat(state.received).isEmpty()
            assertThat(state.sent.map { it.handle }).containsExactly(2L)
            assertThat(state.selectedTab).isEqualTo(ContactRequestTab.Received)
        }
    }

    @Test
    fun `test that uiState keeps the user selection when data re-emits after selectTab`() =
        runTest {
            val requests = MutableSharedFlow<ContactRequestLists>(replay = 1)
            requests.tryEmit(
                ContactRequestLists(
                    incomingContactRequests = listOf(createRequest(1L, isOutgoing = false)),
                    outgoingContactRequests = emptyList(),
                )
            )
            whenever(monitorContactRequestsUseCase()).thenReturn(requests)
            initUnderTest(initialTab = ContactRequestTab.Received)

            underTest.uiState.test {
                awaitItem()
                underTest.selectTab(ContactRequestTab.Sent)
                assertThat((awaitItem() as ContactRequestsUiState.Data).selectedTab)
                    .isEqualTo(ContactRequestTab.Sent)

                requests.emit(
                    ContactRequestLists(
                        incomingContactRequests = listOf(
                            createRequest(1L, isOutgoing = false),
                            createRequest(3L, isOutgoing = false),
                        ),
                        outgoingContactRequests = emptyList(),
                    )
                )

                val state = awaitItem() as ContactRequestsUiState.Data
                assertThat(state.received.map { it.handle }).containsExactly(1L, 3L)
                assertThat(state.selectedTab).isEqualTo(ContactRequestTab.Sent)
            }
        }

    @Test
    fun `test that handleAction replies to the contact request`() = runTest {
        initUnderTest()
        val item = ContactRequestUiItem(
            handle = 7L,
            isOutgoing = true,
            contact = contactRequestUiItemMapper(createRequest(7L, isOutgoing = true)).contact,
            createdTime = "",
        )

        underTest.handleAction(item, ContactRequestAction.Delete)

        verify(replyContactRequestUseCase).invoke(7L, ContactRequestAction.Delete, true)
    }

    @Test
    fun `test that init does not reply to any contact request`() = runTest {
        initUnderTest()

        underTest.uiState.test { awaitItem() }

        verifyNoInteractions(replyContactRequestUseCase)
    }

    private fun stubRequests(
        incoming: List<ContactRequest>,
        outgoing: List<ContactRequest>,
    ) {
        whenever(monitorContactRequestsUseCase()).thenReturn(
            flowOf(
                ContactRequestLists(
                    incomingContactRequests = incoming,
                    outgoingContactRequests = outgoing,
                )
            )
        )
    }

    private fun createRequest(
        handle: Long,
        isOutgoing: Boolean,
        sourceEmail: String = "source@example.com",
        targetEmail: String = "target@example.com",
    ) = ContactRequest(
        handle = handle,
        sourceEmail = sourceEmail,
        sourceMessage = null,
        targetEmail = targetEmail,
        creationTime = 1_700_000_000L,
        modificationTime = 1_700_000_000L,
        status = ContactRequestStatus.Unresolved,
        isOutgoing = isOutgoing,
        isAutoAccepted = false,
    )

    private companion object {
        const val UI_STATE_STOP_TIMEOUT_MS = 5_000L
    }
}
