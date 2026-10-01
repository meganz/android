package mega.privacy.android.domain.usecase.account

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class MonitorAccountBlockedUseCaseTest {
    lateinit var underTest: MonitorAccountBlockedUseCase

    private val notificationsRepository = mock<NotificationsRepository>()

    @BeforeEach
    fun setUp() {
        underTest = MonitorAccountBlockedUseCase(notificationsRepository = notificationsRepository)
    }

    @Test
    fun `test that initial unblocked status is emitted`() = runTest {
        notificationsRepository.stub { on { monitorEvent() } doReturn flow { awaitCancellation() } }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(
                AccountBlockedEvent(
                    -1L,
                    AccountBlockedType.NOT_BLOCKED,
                    ""
                )
            )
        }
    }

    @Test
    fun `test that subsequent events are emitted`() = runTest {
        val expectedEvents = listOf(
            AccountBlockedEvent(1L, AccountBlockedType.TOS_COPYRIGHT, ""),
            AccountBlockedEvent(2L, AccountBlockedType.VERIFICATION_SMS, "")
        )

        notificationsRepository.stub {
            on { monitorEvent() } doReturn flow {
                expectedEvents.onEach { emit(it) }
                awaitCancellation()
            }
        }

        underTest().drop(1).test {
            expectedEvents.forEach {
                assertThat(awaitItem()).isEqualTo(it)
            }
        }
    }
}