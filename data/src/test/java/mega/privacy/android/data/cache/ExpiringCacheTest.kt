package mega.privacy.android.data.cache

import mega.privacy.android.data.gateway.DeviceGateway
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ExpiringCacheTest {
    private val deviceGateway = mock<DeviceGateway>()

    @Test
    fun `test that cache valid then return not null`() {
        val underTest = ExpiringCache<Any>(deviceGateway, TIME_OUT)
        whenever(deviceGateway.getElapsedRealtime()).thenReturn(2000)
        underTest.set(Any())
        whenever(deviceGateway.getElapsedRealtime()).thenReturn(2000 + TIME_OUT - 1)
        assertNotNull(underTest.get())
    }

    @Test
    fun `test that cache invalid then return null`() {
        val underTest = ExpiringCache<Any>(deviceGateway, TIME_OUT)
        whenever(deviceGateway.getElapsedRealtime()).thenReturn(2000)
        underTest.set(Any())
        whenever(deviceGateway.getElapsedRealtime()).thenReturn(2000 + TIME_OUT + 1)
        assertNull(underTest.get())
    }

    companion object {
        private const val TIME_OUT = 1000L
    }
}