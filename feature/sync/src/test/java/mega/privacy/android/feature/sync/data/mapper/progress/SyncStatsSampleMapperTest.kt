package mega.privacy.android.feature.sync.data.mapper.progress

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncStatsSampleMapperTest {

    private lateinit var underTest: SyncStatsSampleMapper

    @BeforeEach
    fun setUp() {
        underTest = SyncStatsSampleMapper()
    }

    @Test
    fun `test that invoke maps the scanning and syncing flags`() {
        val actual = underTest(mockMegaSyncStats(backupId = 1L, isScanning = true, isSyncing = true))

        assertThat(actual.isScanning).isTrue()
        assertThat(actual.isSyncing).isTrue()
    }

    @Test
    fun `test that invoke maps the pending upload and download counts`() {
        val actual = underTest(mockMegaSyncStats(backupId = 1L, uploads = 3, downloads = 7))

        assertThat(actual.pendingUploads).isEqualTo(3)
        assertThat(actual.pendingDownloads).isEqualTo(7)
    }
}
