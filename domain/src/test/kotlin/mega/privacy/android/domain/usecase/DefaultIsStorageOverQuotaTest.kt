package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultIsStorageOverQuotaTest {
    private lateinit var underTest: IsStorageOverQuota

    private val monitorStorageStateEvent = mock<MonitorStorageStateEvent>()

    @Before
    fun setUp() {
        underTest = DefaultIsStorageOverQuota(
            monitorStorageStateEvent = monitorStorageStateEvent
        )
    }

    @Test
    fun `test that paywall event returns true`() = runTest {
        val storageState = StorageState.PayWall
        val event = storageStateEvent(storageState)
        whenever(monitorStorageStateEvent()).thenReturn(MutableStateFlow(event))

        assertThat(underTest()).isTrue()
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

    @Test
    fun `test that non paywall states return false`() = runTest {
        StorageState.values().filterNot { it == StorageState.PayWall }
            .forEach {
                val event = storageStateEvent(it)
                whenever(monitorStorageStateEvent()).thenReturn(MutableStateFlow(event))

                assertThat(underTest()).isFalse()
            }
    }
}