package mega.privacy.android.domain.usecase.account

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.stub

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorStorageStateUseCaseTest {
    private lateinit var underTest: MonitorStorageStateUseCase

    private val notificationsRepository = mock<NotificationsRepository>()
    private val getCurrentStorageStateUseCase = mock<GetCurrentStorageStateUseCase>()

    @BeforeEach
    fun setup() {
        underTest = MonitorStorageStateUseCase(
            notificationsRepository,
            getCurrentStorageStateUseCase,
        )
    }


    @Test
    fun `test that current state is emitted`() = runTest {
        val expected = StorageState.Unknown
        getCurrentStorageStateUseCase.stub { onBlocking { invoke() }.thenReturn(expected) }
        notificationsRepository.stub { on { monitorEvent() }.thenReturn(flow { awaitCancellation() }) }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    fun `test that subsequent updates are emitted`() = runTest {
        val expected = StorageState.Green
        getCurrentStorageStateUseCase.stub { onBlocking { invoke() }.thenReturn(StorageState.Unknown) }
        notificationsRepository.stub {
            on { monitorEvent() }.thenReturn(flow {
                emit(StorageStateEvent(1L, "", 0L, "", EventType.Storage, expected))
                awaitCancellation()
            })
        }

        underTest().test {
            val actual = cancelAndConsumeRemainingEvents()
                .filterIsInstance<Event.Item<StorageState>>()
                .last().value
            assertThat(actual).isEqualTo(expected)
        }
    }


}