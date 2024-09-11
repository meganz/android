package mega.privacy.android.feature.sync.data.mapper.sync

import androidx.work.NetworkType
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.data.mapper.SyncByWifiToNetworkTypeMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncByWifiToNetworkTypeMapperTest {

    private val underTest = SyncByWifiToNetworkTypeMapper()

    @Test
    fun `test that the network type is connected if sync only by wifi is disabled`() {
        val networkType = underTest(false)

        assertThat(networkType).isEqualTo(NetworkType.CONNECTED)
    }

    @Test
    fun `test that the network type is unmetered if sync only by wifi is enabled`() {
        val networkType = underTest(true)

        assertThat(networkType).isEqualTo(NetworkType.UNMETERED)
    }
}
