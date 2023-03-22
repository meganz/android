package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.usecase.DefaultIsNotEnoughQuota
import mega.privacy.android.domain.usecase.IsNotEnoughQuota
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultIsNotEnoughQuotaTest {
    private lateinit var underTest: IsNotEnoughQuota

    private val monitorStorageStateEventUseCase = mock<MonitorStorageStateEventUseCase>()

    @Before
    fun setUp() {
        underTest = DefaultIsNotEnoughQuota(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase
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
        StorageState.values().filterNot { it == StorageState.Red }
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
