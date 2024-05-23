package mega.privacy.android.domain.usecase.account.contactrequest

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactRequest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class MonitorContactRequestsUseCaseTest {
    private lateinit var underTest: MonitorContactRequestsUseCase


    private val getIncomingContactRequestsUseCase = mock<GetIncomingContactRequestsUseCase>()
    private val getOutgoingContactRequestsUseCase = mock<GetOutgoingContactRequestsUseCase>()
    private val monitorContactRequestUpdatesUseCase = mock<MonitorContactRequestUpdatesUseCase>()

    private fun initUnderTest() {
        underTest = MonitorContactRequestsUseCase(
            getIncomingContactRequestsUseCase = getIncomingContactRequestsUseCase,
            getOutgoingContactRequestsUseCase = getOutgoingContactRequestsUseCase,
            monitorContactRequestUpdatesUseCase = monitorContactRequestUpdatesUseCase
        )
    }

    @Test
    internal fun `test that initial values are emitted`() = runTest {
        val incomingContactRequests = listOf(mock<ContactRequest>())
        val outgoingContactRequests = listOf(mock<ContactRequest>())
        getIncomingContactRequestsUseCase.stub {
            onBlocking { invoke() } doReturn incomingContactRequests
        }
        getOutgoingContactRequestsUseCase.stub {
            onBlocking { invoke() } doReturn outgoingContactRequests
        }
        monitorContactRequestUpdatesUseCase.stub {
            on { invoke() } doReturn flow { awaitCancellation() }
        }

        initUnderTest()

        underTest().test {
            val actual = awaitItem()
            assertThat(actual.incomingContactRequests).isEqualTo(incomingContactRequests)
            assertThat(actual.outgoingContactRequests).isEqualTo(outgoingContactRequests)
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @Test
    internal fun `test that new values are emitted when an update is received`() = runTest {
        val incomingContactRequests = listOf(mock<ContactRequest>())
        val outgoingContactRequests = listOf(mock<ContactRequest>())
        getIncomingContactRequestsUseCase.stub {
            onBlocking { invoke() }.thenReturn(emptyList(), incomingContactRequests)
        }
        getOutgoingContactRequestsUseCase.stub {
            onBlocking { invoke() }.thenReturn(emptyList(), outgoingContactRequests)
        }
        val update = mock<ContactRequest>()
        monitorContactRequestUpdatesUseCase.stub {
            on { invoke() } doReturn flow {
                emit(listOf(update))
                awaitCancellation()
            }
        }

        initUnderTest()

        underTest().test {
            val initial = awaitItem()
            assertThat(initial.incomingContactRequests).isEmpty()
            assertThat(initial.outgoingContactRequests).isEmpty()
            val actual = awaitItem()
            assertThat(actual.incomingContactRequests).isEqualTo(incomingContactRequests)
            assertThat(actual.outgoingContactRequests).isEqualTo(outgoingContactRequests)
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

}