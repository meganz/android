package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsStorageOverQuotaUseCaseTest {
    private lateinit var underTest: IsStorageOverQuotaUseCase

    private val monitorStorageStateEventUseCase = mock<MonitorStorageStateEventUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = IsStorageOverQuotaUseCase(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorStorageStateEventUseCase,
        )
    }

    @Test
    fun `test that almost full storage event returns true`() = runTest {
        val storageState = StorageState.Red
        val event = storageStateEvent(storageState)
        whenever(monitorStorageStateEventUseCase()).thenReturn(MutableStateFlow(event))

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that enough storage states return false`() = runTest {
        StorageState.entries.filterNot { it == StorageState.Red }
            .forEach {
                val event = storageStateEvent(it)
                whenever(monitorStorageStateEventUseCase()).thenReturn(MutableStateFlow(event))

                assertThat(underTest()).isFalse()
            }
    }

    private fun storageStateEvent(storageState: StorageState) =
        StorageStateEvent(
            handle = 1L,
            eventString = "eventString",
            number = 0L,
            text = "text",
            type = EventType.Storage,
            storageState = storageState
        )
}
