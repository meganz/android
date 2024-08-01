package mega.privacy.android.feature.sync.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncDebris
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.repository.SyncDebrisRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.GetSyncDebrisSizeInBytesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSyncDebrisSizeInBytesUseCaseTest {

    private lateinit var underTest: GetSyncDebrisSizeInBytesUseCase

    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()
    private val syncDebrisRepository: SyncDebrisRepository = mock()


    @BeforeAll
    fun setup() {
        underTest = GetSyncDebrisSizeInBytesUseCase(monitorSyncsUseCase, syncDebrisRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorSyncsUseCase, syncDebrisRepository
        )
    }

    @Test
    fun `test that get sync debris size correctly calculates the size`() = runTest {
        val firstSync = FolderPair(
            id = 343L,
            pairName = "Sync",
            localFolderPath = "/storage/emulated/0/Sync",
            remoteFolder = RemoteFolder(1244L, "sync_mobile"),
            syncStatus = SyncStatus.SYNCED,
        )
        val secondSync = FolderPair(
            id = 6886L,
            pairName = "Trip_to_NZ",
            localFolderPath = "/storage/emulated/0/Trip_to_nz",
            remoteFolder = RemoteFolder(1244L, "NZ_trip"),
            syncStatus = SyncStatus.SYNCED,
        )
        val firstSyncDebrisSizeInBytes = 1000L
        val secondSyncDebrisSizeInBytes = 2000L
        val syncs = listOf(
            firstSync, secondSync
        )
        val expectedDebrisSize = firstSyncDebrisSizeInBytes + secondSyncDebrisSizeInBytes
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(syncDebrisRepository.getSyncDebrisForSyncs(syncs)).thenReturn(
            listOf(
                SyncDebris(firstSync.id, firstSync.localFolderPath, firstSyncDebrisSizeInBytes),
                SyncDebris(secondSync.id, secondSync.localFolderPath, secondSyncDebrisSizeInBytes),
            )
        )

        val actualDebrisSize = underTest()

        assertThat(actualDebrisSize).isEqualTo(expectedDebrisSize)
    }
}