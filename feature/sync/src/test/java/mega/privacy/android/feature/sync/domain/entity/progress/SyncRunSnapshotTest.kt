package mega.privacy.android.feature.sync.domain.entity.progress

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncRunSnapshotTest {

    private fun sync(
        syncId: Long = 1L,
        runState: SyncRunState = SyncRunState.Running,
        stats: SyncStatsSample? = sample(),
        hasError: Boolean = false,
    ) = SyncWorkState(
        syncId = syncId,
        runState = runState,
        isStorageOverQuota = false,
        stats = stats,
        runningSinceMillis = null,
        hasError = hasError,
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

    private fun seeded(vararg syncs: SyncWorkState) = SyncRunSnapshot(
        isSeeded = true,
        syncs = syncs.toList(),
        progress = SyncEpisodeProgress.None,
    )

    @Test
    fun `test that hasTransferWork is true when the run snapshot is not seeded`() {
        assertThat(SyncRunSnapshot.NotSeeded.hasTransferWork).isTrue()
    }

    @Test
    fun `test that hasTransferWork is false when no sync exists`() {
        assertThat(seeded().hasTransferWork).isFalse()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SyncRunState::class, names = ["Pending", "Loading"])
    fun `test that hasTransferWork is true when a sync is pending or loading`(state: SyncRunState) {
        assertThat(seeded(sync(runState = state, stats = null)).hasTransferWork).isTrue()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SyncRunState::class, names = ["Pending", "Loading"])
    fun `test that hasTransferWork is true when a sync with an error is pending or loading`(
        state: SyncRunState,
    ) {
        assertThat(seeded(sync(runState = state, stats = null, hasError = true)).hasTransferWork)
            .isTrue()
    }

    @Test
    fun `test that hasTransferWork is true when a running sync reports pending items`() {
        assertThat(seeded(sync(stats = sample(downloads = 3))).hasTransferWork).isTrue()
    }

    @Test
    fun `test that hasTransferWork is true when a running sync reports scanning`() {
        assertThat(seeded(sync(stats = sample(scanning = true))).hasTransferWork).isTrue()
    }

    @Test
    fun `test that hasTransferWork is false when a running sync reports only syncing`() {
        assertThat(seeded(sync(stats = sample(syncing = true))).hasTransferWork).isFalse()
    }

    @Test
    fun `test that hasTransferWork is false when a running sync has no stats yet`() {
        assertThat(seeded(sync(stats = null)).hasTransferWork).isFalse()
    }

    @Test
    fun `test that hasTransferWork is false when a running sync with an error reports pending items`() {
        val stats = sample(scanning = true, uploads = 5)

        assertThat(seeded(sync(hasError = true, stats = stats)).hasTransferWork).isFalse()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SyncRunState::class, names = ["Suspended", "Disabled"])
    fun `test that hasTransferWork is false when a sync is suspended or disabled`(
        state: SyncRunState,
    ) {
        val stats = sample(scanning = true, uploads = 5)

        assertThat(seeded(sync(runState = state, stats = stats)).hasTransferWork).isFalse()
    }

    @Test
    fun `test that hasTransferWork is true when one of several syncs has work`() {
        val snapshot = seeded(
            sync(syncId = 1),
            sync(syncId = 2, stats = sample(uploads = 1)),
        )

        assertThat(snapshot.hasTransferWork).isTrue()
    }
}
