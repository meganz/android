package mega.privacy.android.feature.sync.data.mapper.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.backup.SyncErrorMapper
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import nz.mega.sdk.MegaSync
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncWorkStateMapperTest {

    private lateinit var underTest: SyncWorkStateMapper

    private val syncErrorMapper = mock<SyncErrorMapper>()

    private val sample = SyncStatsSample(
        isScanning = false,
        isSyncing = true,
        pendingUploads = 2,
        pendingDownloads = 1,
    )

    @BeforeEach
    fun setUp() {
        underTest = SyncWorkStateMapper(
            syncRunStateMapper = SyncRunStateMapper(),
            syncErrorMapper = syncErrorMapper,
        )
        whenever(syncErrorMapper(any())).thenReturn(SyncError.NO_SYNC_ERROR)
    }

    @AfterEach
    fun resetMocks() {
        reset(syncErrorMapper)
    }

    @Test
    fun `test that invoke maps the backup id and run state`() {
        val sync = mockMegaSync(
            backupId = 7L,
            runState = MegaSync.SyncRunningState.RUNSTATE_SUSPENDED.swigValue(),
        )

        val actual = underTest(sync, stats = null, runningSinceMillis = null)

        assertThat(actual.syncId).isEqualTo(7L)
        assertThat(actual.runState).isEqualTo(SyncRunState.Suspended)
    }

    @Test
    fun `test that invoke marks storage over quota from the sync error`() {
        val overQuota = mockMegaSync(backupId = 1L, error = 22)
        val otherError = mockMegaSync(backupId = 2L, error = 1)
        whenever(syncErrorMapper(22)).thenReturn(SyncError.STORAGE_OVERQUOTA)
        whenever(syncErrorMapper(1)).thenReturn(SyncError.UNKNOWN_ERROR)

        assertThat(underTest(overQuota, null, null).isStorageOverQuota).isTrue()
        assertThat(underTest(otherError, null, null).isStorageOverQuota).isFalse()
    }

    @Test
    fun `test that invoke keeps stats and running since time when running`() {
        val actual = underTest(mockMegaSync(backupId = 1L), stats = sample, runningSinceMillis = 42L)

        assertThat(actual.stats).isEqualTo(sample)
        assertThat(actual.runningSinceMillis).isEqualTo(42L)
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = [0, 1, 4, 5])
    fun `test that invoke drops stats and running since time when not running`(runState: Int) {
        val actual = underTest(
            mockMegaSync(backupId = 1L, runState = runState),
            stats = sample,
            runningSinceMillis = 42L,
        )

        assertThat(actual.stats).isNull()
        assertThat(actual.runningSinceMillis).isNull()
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = [2, 4, 5])
    fun `test that invoke marks hasError when the sync reports an error`(runState: Int) {
        val sync = mockMegaSync(
            backupId = 1L,
            runState = runState,
            error = MegaSync.Error.UNKNOWN_ERROR.swigValue(),
        )
        whenever(syncErrorMapper(MegaSync.Error.UNKNOWN_ERROR.swigValue()))
            .thenReturn(SyncError.UNKNOWN_ERROR)

        assertThat(underTest(sync, null, null).hasError).isTrue()
    }

    @Test
    fun `test that invoke does not mark hasError when the sync reports no error`() {
        val sync = mockMegaSync(backupId = 1L, error = MegaSync.Error.NO_SYNC_ERROR.swigValue())
        whenever(syncErrorMapper(MegaSync.Error.NO_SYNC_ERROR.swigValue()))
            .thenReturn(SyncError.NO_SYNC_ERROR)

        assertThat(underTest(sync, null, null).hasError).isFalse()
    }

    @Test
    fun `test that invoke reads the sync error once`() {
        val sync = mockMegaSync(backupId = 1L, error = MegaSync.Error.UNKNOWN_ERROR.swigValue())
        whenever(syncErrorMapper(MegaSync.Error.UNKNOWN_ERROR.swigValue()))
            .thenReturn(SyncError.UNKNOWN_ERROR)

        val actual = underTest(sync, null, null)

        assertThat(actual.hasError).isTrue()
        assertThat(actual.isStorageOverQuota).isFalse()
        verify(sync, times(1)).error
    }
}
