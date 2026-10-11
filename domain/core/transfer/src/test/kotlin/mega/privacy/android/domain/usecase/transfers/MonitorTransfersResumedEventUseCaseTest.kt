package mega.privacy.android.domain.usecase.transfers

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.CommitDbEvent
import mega.privacy.android.domain.entity.TransfersResumedEvent
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorTransfersResumedEventUseCaseTest {

    private lateinit var underTest: MonitorTransfersResumedEventUseCase

    private val notificationsRepository = mock<NotificationsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorTransfersResumedEventUseCase(
            notificationsRepository = notificationsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(notificationsRepository)
    }

    @Test
    fun `test that invoke emits uniqueIds when repository emits TransfersResumedEvent`() =
        runTest {
            val uniqueIds = listOf(1, 2, 3)
            whenever(notificationsRepository.monitorEvent()).thenReturn(
                flowOf(TransfersResumedEvent(handle = 0L, uniqueIds = uniqueIds))
            )

            underTest().test {
                assertThat(awaitItem()).isEqualTo(uniqueIds)
                awaitComplete()
            }
        }

    @Test
    fun `test that invoke filters out events that are not TransfersResumedEvent`() = runTest {
        whenever(notificationsRepository.monitorEvent()).thenReturn(
            flowOf(CommitDbEvent(handle = 0L))
        )

        underTest().test {
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits only TransfersResumedEvent uniqueIds when repository emits mixed events`() =
        runTest {
            val firstIds = listOf(10, 20)
            val secondIds = listOf(30)
            whenever(notificationsRepository.monitorEvent()).thenReturn(
                flowOf(
                    TransfersResumedEvent(handle = 0L, uniqueIds = firstIds),
                    CommitDbEvent(handle = 0L),
                    TransfersResumedEvent(handle = 0L, uniqueIds = secondIds),
                )
            )

            underTest().test {
                assertThat(awaitItem()).isEqualTo(firstIds)
                assertThat(awaitItem()).isEqualTo(secondIds)
                awaitComplete()
            }
        }

    @Test
    fun `test that invoke does not emit when TransfersResumedEvent has empty uniqueIds`() =
        runTest {
            whenever(notificationsRepository.monitorEvent()).thenReturn(
                flowOf(TransfersResumedEvent(handle = 0L, uniqueIds = emptyList()))
            )

            underTest().test {
                awaitComplete()
            }
        }
}
