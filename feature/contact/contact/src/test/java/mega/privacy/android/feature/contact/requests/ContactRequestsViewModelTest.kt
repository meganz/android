package mega.privacy.android.feature.contact.requests

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
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

    private fun initUnderTest(initialTab: ContactRequestTab = ContactRequestTab.Received) {
        underTest = ContactRequestsViewModel(
            initialTab = initialTab,
            monitorContactRequestsUseCase = monitorContactRequestsUseCase,
            replyContactRequestUseCase = replyContactRequestUseCase,
            contactRequestUiItemMapper = contactRequestUiItemMapper,
        )
    }

    @BeforeEach
    fun setUp() {
        whenever(monitorContactRequestsUseCase()).thenReturn(
            flowOf(ContactRequestLists(emptyList(), emptyList()))
        )
    }

    @AfterEach
    fun tearDown() {
        reset(monitorContactRequestsUseCase, replyContactRequestUseCase)
    }

    @Test
    fun `test that uiState maps incoming requests to received and outgoing requests to sent`() =
        runTest {
            whenever(monitorContactRequestsUseCase()).thenReturn(
                flowOf(
                    ContactRequestLists(
                        incomingContactRequests = listOf(createRequest(1L, isOutgoing = false)),
                        outgoingContactRequests = listOf(createRequest(2L, isOutgoing = true)),
                    )
                )
            )
            initUnderTest()

            underTest.uiState.test {
                val state = awaitItem() as ContactRequestsUiState.Data
                assertThat(state.received.map { it.handle }).containsExactly(1L)
                assertThat(state.sent.map { it.handle }).containsExactly(2L)
                assertThat(state.selectedTab).isEqualTo(ContactRequestTab.Received)
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

    private fun createRequest(
        handle: Long,
        isOutgoing: Boolean,
    ) = ContactRequest(
        handle = handle,
        sourceEmail = "source@example.com",
        sourceMessage = null,
        targetEmail = "target@example.com",
        creationTime = 1_700_000_000L,
        modificationTime = 1_700_000_000L,
        status = ContactRequestStatus.Unresolved,
        isOutgoing = isOutgoing,
        isAutoAccepted = false,
    )
}
