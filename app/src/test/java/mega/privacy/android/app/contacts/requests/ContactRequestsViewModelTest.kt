package mega.privacy.android.app.contacts.requests

import android.graphics.drawable.Drawable
import android.net.Uri
import com.jraska.livedata.test
import io.reactivex.rxjava3.core.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.contacts.usecase.GetContactRequestsUseCase
import mega.privacy.android.app.contacts.usecase.ManageContactRequestUseCase
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.domain.entity.contacts.ContactRequestLists
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking
import test.mega.privacy.android.app.InstantExecutorExtension
import test.mega.privacy.android.app.TestSchedulerExtension
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(
    InstantExecutorExtension::class,
    CoroutineMainDispatcherExtension::class,
    TestSchedulerExtension::class
)
class ContactRequestsViewModelTest {
    private lateinit var underTest: ContactRequestsViewModel

    private val getContactRequestsUseCase = mock<GetContactRequestsUseCase>()
    private val manageContactRequestUseCase = mock<ManageContactRequestUseCase>()
    private val monitorContactRequestsUseCase = mock<MonitorContactRequestsUseCase>()

    @BeforeEach
    fun setUp() {
        reset(
            getContactRequestsUseCase,
            manageContactRequestUseCase,
            monitorContactRequestsUseCase,
        )
        getContactRequestsUseCase.stub {
            on { get() }.thenReturn(
                Flowable.empty()
            )
        }
    }

    private fun initUnderTest() {
        underTest = ContactRequestsViewModel(
            getContactRequestsUseCase,
            manageContactRequestUseCase,
            monitorContactRequestsUseCase,
        )
    }

    @Test
    fun `handleContactRequest should call manageContactRequestUseCase`() {
        val requestHandle = 1L
        val action = ContactRequestAction.Accept
        getContactRequestsUseCase.stub {
            on { get() }.thenReturn(Flowable.empty())
        }
        initUnderTest()

        underTest.handleContactRequest(requestHandle, action)

        verifyBlocking(manageContactRequestUseCase) { invoke(requestHandle, action) }
    }

    @Test
    fun `getFilteredContactRequests should filter by query`() = runTest {
        val contactRequests = listOf(
            ContactRequestItem(1L, "test1@test.com", Uri.EMPTY, mock<Drawable>(), "time", true),
            ContactRequestItem(1L, "test2@test.com", Uri.EMPTY, mock<Drawable>(), "time", false)
        )

        getContactRequestsUseCase.stub {
            on { get() }.thenReturn(
                Flowable.just(contactRequests)
            )
        }
        initUnderTest()

        advanceUntilIdle()
        val testObserver = underTest.getFilteredContactRequests().test()
        testObserver.awaitValue(2, TimeUnit.SECONDS).assertValue(contactRequests)
        underTest.setQuery("test1")
        testObserver.awaitNextValue(2, TimeUnit.SECONDS).assertValue(listOf(contactRequests[0]))
    }

    @Test
    fun `getIncomingRequest should filter by isOutgoing`() {
        val contactRequests = listOf(
            ContactRequestItem(1L, "test1@test.com", Uri.EMPTY, mock<Drawable>(), "time", true),
            ContactRequestItem(1L, "test2@test.com", Uri.EMPTY, mock<Drawable>(), "time", false)
        )
        getContactRequestsUseCase.stub {
            on { get() }.thenReturn(
                Flowable.just(contactRequests)
            )
        }
        initUnderTest()

        underTest.getIncomingRequest().test().awaitValue().assertValue(listOf(contactRequests[1]))
    }

    @Test
    fun `getOutgoingRequest should filter by isOutgoing`() {
        val contactRequests = listOf(
            ContactRequestItem(1L, "test1@test.com", Uri.EMPTY, mock<Drawable>(), "time", true),
            ContactRequestItem(1L, "test2@test.com", Uri.EMPTY, mock<Drawable>(), "time", false)
        )
        getContactRequestsUseCase.stub {
            on { get() }.thenReturn(
                Flowable.just(contactRequests)
            )
        }
        initUnderTest()

        underTest.getOutgoingRequest().test().awaitValue().assertValue(listOf(contactRequests[0]))
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

    private fun stubRequestCounts(incomingCount: Int, outgoingCount: Int) {
        val requestLists = ContactRequestLists(
            incomingContactRequests = List(incomingCount) { mock<ContactRequest>() },
            outgoingContactRequests = List(outgoingCount) { mock<ContactRequest>() }
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
    }
}