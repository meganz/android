package mega.privacy.android.feature.sync.domain.entity.progress

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncWorkStateTest {

    private fun workState(runState: SyncRunState, stats: SyncStatsSample? = null) = SyncWorkState(
        syncId = 1L,
        runState = runState,
        isStorageOverQuota = false,
        stats = stats,
        runningSinceMillis = null,
    )

    private fun sample(
        scanning: Boolean = false,
        syncing: Boolean = false,
        uploads: Int = 0,
        downloads: Int = 0,
    ) = SyncStatsSample(
        isScanning = scanning,
        isSyncing = syncing,
        pendingUploads = uploads,
        pendingDownloads = downloads,
    )

    @Test
    fun `test that hasWork is true when the sync is pending`() {
        assertThat(workState(SyncRunState.Pending).hasWork).isTrue()
    }

    @Test
    fun `test that hasWork is true when the sync is loading`() {
        assertThat(workState(SyncRunState.Loading).hasWork).isTrue()
    }

    @Test
    fun `test that hasWork is true when a running sync is scanning`() {
        assertThat(workState(SyncRunState.Running, sample(scanning = true)).hasWork).isTrue()
    }

    @Test
    fun `test that hasWork is true when a running sync has pending items`() {
        assertThat(workState(SyncRunState.Running, sample(uploads = 3)).hasWork).isTrue()
    }

    @Test
    fun `test that hasWork is false when only isSyncing is set`() {
        assertThat(workState(SyncRunState.Running, sample(syncing = true)).hasWork).isFalse()
    }

    @Test
    fun `test that hasWork is false when a running sync has no stats yet`() {
        assertThat(workState(SyncRunState.Running, stats = null).hasWork).isFalse()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SyncRunState::class, names = ["Suspended", "Disabled"])
    fun `test that hasWork is false when the sync is suspended or disabled`(state: SyncRunState) {
        assertThat(workState(state).hasWork).isFalse()
    }

    @Test
    fun `test that pendingItems sums uploads and downloads`() {
        assertThat(sample(uploads = 2, downloads = 3).pendingItems).isEqualTo(5)
    }
}
