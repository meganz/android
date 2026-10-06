package mega.privacy.android.feature.sync.data.gateway

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.data.mapper.progress.mockMegaSyncStats
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncStatsCacheGatewayImplTest {

    private lateinit var underTest: SyncStatsCacheGatewayImpl

    @BeforeEach
    fun setUp() {
        underTest = SyncStatsCacheGatewayImpl()
    }

    @Test
    fun `test that getAllSyncStats returns every cached stats`() {
        val first = mockMegaSyncStats(backupId = 1L)
        val second = mockMegaSyncStats(backupId = 2L)

        underTest.setSyncStats(first)
        underTest.setSyncStats(second)

        assertThat(underTest.getAllSyncStats()).containsExactly(first, second)
    }

    @Test
    fun `test that getAllSyncStats returns an empty list when nothing is cached`() {
        assertThat(underTest.getAllSyncStats()).isEmpty()
    }

    @Test
    fun `test that setSyncStats replaces the stats of the same sync`() {
        val older = mockMegaSyncStats(backupId = 1L, uploads = 1)
        val newer = mockMegaSyncStats(backupId = 1L, uploads = 2)

        underTest.setSyncStats(older)
        underTest.setSyncStats(newer)

        assertThat(underTest.getSyncStatsById(1L)).isSameInstanceAs(newer)
        assertThat(underTest.getAllSyncStats()).containsExactly(newer)
    }
}
