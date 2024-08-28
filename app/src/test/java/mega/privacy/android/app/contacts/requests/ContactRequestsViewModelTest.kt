package mega.privacy.android.app.contacts.requests

import android.graphics.drawable.Drawable
import android.net.Uri
import com.jraska.livedata.test
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.contacts.requests.mapper.ContactRequestItemMapper
import mega.privacy.android.app.contacts.usecase.ManageContactRequestUseCase
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.domain.entity.contacts.ContactRequestLists
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking
import mega.privacy.android.app.InstantExecutorExtension
import mega.privacy.android.app.TestSchedulerExtension

@ExtendWith(
    InstantExecutorExtension::class,
    CoroutineMainDispatcherExtension::class,
    TestSchedulerExtension::class
)
class ContactRequestsViewModelTest {
    private lateinit var underTest: ContactRequestsViewModel

    private val manageContactRequestUseCase = mock<ManageContactRequestUseCase>()
    private val monitorContactRequestsUseCase = mock<MonitorContactRequestsUseCase>()
    private val contactRequestItemMapper = mock<ContactRequestItemMapper>()

    @BeforeEach
    fun setUp() {
        reset(
            manageContactRequestUseCase,
            monitorContactRequestsUseCase,
            contactRequestItemMapper,
        )

        monitorContactRequestsUseCase.stub {
            onBlocking { invoke() }.thenReturn(
                flow {
                    awaitCancellation()
                }
            )
        }

        val placeholder = mock<Drawable>()
        contactRequestItemMapper.stub {
            onBlocking { invoke(any()) }.thenAnswer { invocation ->
                val request = invocation.arguments[0] as ContactRequest
                ContactRequestItem(
                    handle = request.handle,
                    email = if (request.isOutgoing) {
                        request.targetEmail ?: ""
                    } else request.sourceEmail,
                    avatarUri = Uri.EMPTY,
                    placeholder = placeholder,
                    createdTime = "time",
                    isOutgoing = request.isOutgoing
                )

            }
        }
    }


    private fun initUnderTest() {
        underTest = ContactRequestsViewModel(
            manageContactRequestUseCase = manageContactRequestUseCase,
            monitorContactRequestsUseCase = monitorContactRequestsUseCase,
            contactRequestItemMapper = contactRequestItemMapper,
        )
    }

    @Test
    fun `handleContactRequest should call manageContactRequestUseCase`() {
        val requestHandle = 1L
        val action = ContactRequestAction.Accept
        initUnderTest()

        underTest.handleContactRequest(requestHandle, action)

        verifyBlocking(manageContactRequestUseCase) { invoke(requestHandle, action) }
    }

    @Test
    fun `getDefaultPagerPosition should return OUTGOING when there are outgoing requests and isOutgoing is true`() {
        val incomingCount = 0
        val outgoingCount = 1
        stubRequestCounts(incomingCount = incomingCount, outgoingCount = outgoingCount)
        initUnderTest()

        val testObserver = underTest.getDefaultPagerPosition(true).test()
        testObserver.awaitValue().assertValue(ContactRequestPageAdapter.Tabs.OUTGOING.ordinal)
    }

    @Test
    fun `getDefaultPagerPosition should return INCOMING when there are incoming requests and isOutgoing is false`() {
        val incomingCount = 1
        val outgoingCount = 0
        stubRequestCounts(incomingCount = incomingCount, outgoingCount = outgoingCount)
        initUnderTest()

        val testObserver = underTest.getDefaultPagerPosition(false).test()
        testObserver.awaitValue().assertValue(ContactRequestPageAdapter.Tabs.INCOMING.ordinal)
    }

    @Test
    fun `getDefaultPagerPosition should return OUTGOING when there are both incoming and outgoing requests and isOutgoing is true`() {
        val incomingCount = 1
        val outgoingCount = 1
        stubRequestCounts(incomingCount = incomingCount, outgoingCount = outgoingCount)
        initUnderTest()

        val testObserver = underTest.getDefaultPagerPosition(true).test()
        testObserver.awaitValue().assertValue(ContactRequestPageAdapter.Tabs.OUTGOING.ordinal)
    }

    @Test
    fun `getDefaultPagerPosition should return INCOMING when there are both incoming and outgoing requests and isOutgoing is false`() {
        val incomingCount = 1
        val outgoingCount = 1
        stubRequestCounts(incomingCount = incomingCount, outgoingCount = outgoingCount)
        initUnderTest()

        val testObserver = underTest.getDefaultPagerPosition(false).test()
        testObserver.awaitValue().assertValue(ContactRequestPageAdapter.Tabs.INCOMING.ordinal)
    }

    @Test
    fun `getDefaultPagerPosition should return OUTGOING when there are no requests and isOutgoing is true`() {
        val incomingCount = 0
        val outgoingCount = 0
        stubRequestCounts(incomingCount = incomingCount, outgoingCount = outgoingCount)
        initUnderTest()

        val testObserver = underTest.getDefaultPagerPosition(true).test()
        testObserver.awaitValue().assertValue(ContactRequestPageAdapter.Tabs.OUTGOING.ordinal)
    }

    @Test
    fun `getDefaultPagerPosition should return INCOMING when there are no requests and isOutgoing is false`() {
        val incomingCount = 0
        val outgoingCount = 0
        stubRequestCounts(incomingCount = incomingCount, outgoingCount = outgoingCount)
        initUnderTest()

        val testObserver = underTest.getDefaultPagerPosition(false).test()
        testObserver.awaitValue().assertValue(ContactRequestPageAdapter.Tabs.INCOMING.ordinal)
    }

    @Test
    fun `test that incoming requests are set when returned`() {
        val expectedSize = 4
        stubRequestCounts(incomingCount = expectedSize, outgoingCount = 0)

        initUnderTest()
        underTest.getIncomingRequest().test().awaitValue()
            .assertValue { it.size == expectedSize }
    }

    @Test
    fun `test that outgoing requests are set when returned`() {
        val expectedSize = 4
        stubRequestCounts(incomingCount = 0, outgoingCount = expectedSize)

        initUnderTest()
        underTest.getOutgoingRequest().test().awaitValue()
            .assertValue { it.size == expectedSize }
    }

    @Test
    fun `test that incoming requests are filtered`() {
        val expectedSize = 4
        val query = stubRequestCounts(
            incomingCount = expectedSize,
            outgoingCount = 0
        ).incomingContactRequests[0].sourceEmail

        initUnderTest()
        val testObserver = underTest.getIncomingRequest().test()
        testObserver.awaitValue()
            .assertValue { it.size == expectedSize }
        underTest.setQuery(query)
        testObserver.awaitValue().assertValue { it.size == 1 }
    }

    @Test
    fun `test that outgoing requests are filtered`() {
        val expectedSize = 4
        val query = stubRequestCounts(
            incomingCount = 0,
            outgoingCount = expectedSize
        ).outgoingContactRequests[0].targetEmail

        initUnderTest()
        val testObserver = underTest.getOutgoingRequest().test()
        testObserver.awaitValue()
            .assertValue { it.size == expectedSize }
        underTest.setQuery(query)
        testObserver.awaitValue().assertValue { it.size == 1 }
    }

    @Test
    fun `test that get request returns value if found`() {
        val expected = stubRequestCounts(
            incomingCount = 1,
            outgoingCount = 0
        ).incomingContactRequests[0].handle

        initUnderTest()

        underTest.getContactRequest(expected).test().awaitValue()
            .assertValue { it?.handle == expected }
    }

    private fun stubRequestCounts(incomingCount: Int, outgoingCount: Int): ContactRequestLists {
        val requestLists = ContactRequestLists(
            incomingContactRequests = List(incomingCount) { index ->
                mock<ContactRequest> {
                    on { sourceEmail } doReturn "sourceEmail$index"
                    on { targetEmail } doReturn "targetEmail$index"
                    on { isOutgoing } doReturn false
                }
            },
            outgoingContactRequests = List(outgoingCount) { index ->
                mock<ContactRequest> {
                    on { sourceEmail } doReturn "sourceEmail$index"
                    on { targetEmail } doReturn "targetEmail$index"
                    on { isOutgoing } doReturn true
                }
            }
        )
        monitorContactRequestsUseCase.stub {
            on { invoke() }.thenReturn(
                flow {
                    emit(
                        requestLists
                    )
                    awaitCancellation()
                }
            )
        }

        return requestLists
    }
}