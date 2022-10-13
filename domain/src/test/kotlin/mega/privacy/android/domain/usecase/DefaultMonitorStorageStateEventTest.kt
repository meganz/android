package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.NormalEvent
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorStorageStateEventTest {

    private lateinit var underTest: DefaultMonitorStorageStateEvent

    private val notificationsRepository = mock<NotificationsRepository>()

    private val exampleStorageStateEvent = StorageStateEvent(
        handle = 1L,
        eventString = "eventString",
        number = 0L,
        text = "text",
        type = EventType.Storage,
        storageState = StorageState.Unknown
    )

    private val exampleNormalEvent = NormalEvent(
        handle = 1L,
        eventString = "eventString",
        number = 0L,
        text = "text",
        type = EventType.CommitDb,
    )

    @Test
    fun `test that initial storage state is Unknown`() = runTest {
        whenever(notificationsRepository.monitorEvent()).thenReturn(flowOf())
        underTest = DefaultMonitorStorageStateEvent(
            notificationsRepository = notificationsRepository,
            scope = this
        )

        underTest.storageState.test {
            assertThat(awaitItem().storageState).isEqualTo(StorageState.Unknown)
        }
    }

    @Test
    fun `test that storage state event is passed`() = runTest {
        whenever(notificationsRepository.monitorEvent()).thenReturn(
            flowOf(
                exampleStorageStateEvent.copy(storageState = StorageState.PayWall)
            )
        )
        underTest = DefaultMonitorStorageStateEvent(
            notificationsRepository = notificationsRepository,
            scope = this
        )

        underTest.storageState.test {
            assertThat(awaitItem().storageState).isEqualTo(StorageState.Unknown)
            assertThat(awaitItem().storageState).isEqualTo(StorageState.PayWall)
        }
    }

    @Test
    fun `test that only storage state events are received if there are multiple types of events`() =
        runTest {
            whenever(notificationsRepository.monitorEvent()).thenReturn(
                flowOf(
                    exampleNormalEvent.copy(type = EventType.CommitDb),
                    exampleStorageStateEvent.copy(type = EventType.Storage,
                        storageState = StorageState.Red),
                    exampleNormalEvent.copy(type = EventType.Disconnect),
                    exampleStorageStateEvent.copy(type = EventType.Storage,
                        storageState = StorageState.Green),
                    exampleStorageStateEvent.copy(type = EventType.Storage,
                        storageState = StorageState.Orange),
                )
            )
            underTest = DefaultMonitorStorageStateEvent(
                notificationsRepository = notificationsRepository,
                scope = this
            )

            underTest.storageState.test {
                assertThat(awaitItem().storageState).isEqualTo(StorageState.Unknown)
                assertThat(awaitItem().storageState).isEqualTo(StorageState.Red)
                assertThat(awaitItem().storageState).isEqualTo(StorageState.Green)
                assertThat(awaitItem().storageState).isEqualTo(StorageState.Orange)
            }
            assertThat(underTest.storageState.value.storageState).isEqualTo(StorageState.Orange)
        }
}