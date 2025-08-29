package mega.privacy.android.feature.sync.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncDebrisSizeInBytesUseCaseTest {

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
    fun `test that get sync debris size correctly calculates the size for path-based syncs`() =
        runTest {
            val firstSync = FolderPair(
                id = 343L,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "Sync",
                localFolderPath = "/storage/emulated/0/Sync",
                remoteFolder = RemoteFolder(NodeId(1244L), "sync_mobile"),
                syncStatus = SyncStatus.SYNCED,
            )
            val secondSync = FolderPair(
                id = 6886L,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "Trip_to_NZ",
                localFolderPath = "/storage/emulated/0/Trip_to_nz",
                remoteFolder = RemoteFolder(NodeId(1244L), "NZ_trip"),
                syncStatus = SyncStatus.SYNCED,
            )
            val firstSyncDebrisSizeInBytes = 1000L
            val secondSyncDebrisSizeInBytes = 2000L
            val syncs = listOf(firstSync, secondSync)
            val expectedDebrisSize = firstSyncDebrisSizeInBytes + secondSyncDebrisSizeInBytes

            whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
            whenever(syncDebrisRepository.getSyncDebrisForSyncs(syncs)).thenReturn(
                listOf(
                    SyncDebris(
                        firstSync.id,
                        UriPath(firstSync.localFolderPath),
                        firstSyncDebrisSizeInBytes
                    ),
                    SyncDebris(
                        secondSync.id,
                        UriPath(secondSync.localFolderPath),
                        secondSyncDebrisSizeInBytes
                    ),
                )
            )

            val actualDebrisSize = underTest()

            assertThat(actualDebrisSize).isEqualTo(expectedDebrisSize)
        }

    @Test
    fun `test that get sync debris size correctly calculates the size for URI-based syncs`() =
        runTest {
            val firstSync = FolderPair(
                id = 343L,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "Sync",
                localFolderPath = "content://com.android.externalstorage.documents/tree/primary%3ASync",
                remoteFolder = RemoteFolder(NodeId(1244L), "sync_mobile"),
                syncStatus = SyncStatus.SYNCED,
            )
            val secondSync = FolderPair(
                id = 6886L,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "Trip_to_NZ",
                localFolderPath = "content://com.android.externalstorage.documents/tree/primary%3ATrip_to_nz",
                remoteFolder = RemoteFolder(NodeId(1244L), "NZ_trip"),
                syncStatus = SyncStatus.SYNCED,
            )
            val firstSyncDebrisSizeInBytes = 1500L
            val secondSyncDebrisSizeInBytes = 2500L
            val syncs = listOf(firstSync, secondSync)
            val expectedDebrisSize = firstSyncDebrisSizeInBytes + secondSyncDebrisSizeInBytes

            whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
            whenever(syncDebrisRepository.getSyncDebrisForSyncs(syncs)).thenReturn(
                listOf(
                    SyncDebris(
                        firstSync.id,
                        UriPath(firstSync.localFolderPath),
                        firstSyncDebrisSizeInBytes
                    ),
                    SyncDebris(
                        secondSync.id,
                        UriPath(secondSync.localFolderPath),
                        secondSyncDebrisSizeInBytes
                    ),
                )
            )

            val actualDebrisSize = underTest()

            assertThat(actualDebrisSize).isEqualTo(expectedDebrisSize)
        }

    @Test
    fun `test that get sync debris size returns zero when no syncs exist`() = runTest {
        val syncs = emptyList<FolderPair>()

        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))

        val actualDebrisSize = underTest()

        assertThat(actualDebrisSize).isEqualTo(0L)
        verify(syncDebrisRepository).getSyncDebrisForSyncs(syncs)
    }

    @Test
    fun `test that get sync debris size returns zero when no debris exists`() = runTest {
        val firstSync = FolderPair(
            id = 343L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Sync",
            localFolderPath = "/storage/emulated/0/Sync",
            remoteFolder = RemoteFolder(NodeId(1244L), "sync_mobile"),
            syncStatus = SyncStatus.SYNCED,
        )
        val syncs = listOf(firstSync)

        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(syncDebrisRepository.getSyncDebrisForSyncs(syncs)).thenReturn(emptyList())

        val actualDebrisSize = underTest()

        assertThat(actualDebrisSize).isEqualTo(0L)
    }

    @Test
    fun `test that get sync debris size handles mixed sync types correctly`() = runTest {
        val pathBasedSync = FolderPair(
            id = 343L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "PathSync",
            localFolderPath = "/storage/emulated/0/Sync",
            remoteFolder = RemoteFolder(NodeId(1244L), "sync_mobile"),
            syncStatus = SyncStatus.SYNCED,
        )
        val uriBasedSync = FolderPair(
            id = 6886L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "UriSync",
            localFolderPath = "content://com.android.externalstorage.documents/tree/primary%3ASync",
            remoteFolder = RemoteFolder(NodeId(1244L), "NZ_trip"),
            syncStatus = SyncStatus.SYNCED,
        )
        val pathBasedDebrisSize = 1000L
        val uriBasedDebrisSize = 2000L
        val syncs = listOf(pathBasedSync, uriBasedSync)
        val expectedDebrisSize = pathBasedDebrisSize + uriBasedDebrisSize

        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(syncDebrisRepository.getSyncDebrisForSyncs(syncs)).thenReturn(
            listOf(
                SyncDebris(
                    pathBasedSync.id,
                    UriPath(pathBasedSync.localFolderPath),
                    pathBasedDebrisSize
                ),
                SyncDebris(
                    uriBasedSync.id,
                    UriPath(uriBasedSync.localFolderPath),
                    uriBasedDebrisSize
                ),
            )
        )

        val actualDebrisSize = underTest()

        assertThat(actualDebrisSize).isEqualTo(expectedDebrisSize)
    }

    @Test
    fun `test that get sync debris size handles large file sizes correctly`() = runTest {
        val firstSync = FolderPair(
            id = 343L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "LargeSync",
            localFolderPath = "/storage/emulated/0/LargeSync",
            remoteFolder = RemoteFolder(NodeId(1244L), "large_sync"),
            syncStatus = SyncStatus.SYNCED,
        )
        val secondSync = FolderPair(
            id = 6886L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "HugeSync",
            localFolderPath = "/storage/emulated/0/HugeSync",
            remoteFolder = RemoteFolder(NodeId(1244L), "huge_sync"),
            syncStatus = SyncStatus.SYNCED,
        )
        val firstSyncDebrisSizeInBytes = Long.MAX_VALUE / 2
        val secondSyncDebrisSizeInBytes = Long.MAX_VALUE / 2
        val syncs = listOf(firstSync, secondSync)
        val expectedDebrisSize = firstSyncDebrisSizeInBytes + secondSyncDebrisSizeInBytes

        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(syncDebrisRepository.getSyncDebrisForSyncs(syncs)).thenReturn(
            listOf(
                SyncDebris(
                    firstSync.id,
                    UriPath(firstSync.localFolderPath),
                    firstSyncDebrisSizeInBytes
                ),
                SyncDebris(
                    secondSync.id,
                    UriPath(secondSync.localFolderPath),
                    secondSyncDebrisSizeInBytes
                ),
            )
        )

        val actualDebrisSize = underTest()

        assertThat(actualDebrisSize).isEqualTo(expectedDebrisSize)
    }

    @Test
    fun `test that get sync debris size handles zero size debris correctly`() = runTest {
        val firstSync = FolderPair(
            id = 343L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "ZeroSync",
            localFolderPath = "/storage/emulated/0/ZeroSync",
            remoteFolder = RemoteFolder(NodeId(1244L), "zero_sync"),
            syncStatus = SyncStatus.SYNCED,
        )
        val secondSync = FolderPair(
            id = 6886L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "EmptySync",
            localFolderPath = "/storage/emulated/0/EmptySync",
            remoteFolder = RemoteFolder(NodeId(1244L), "empty_sync"),
            syncStatus = SyncStatus.SYNCED,
        )
        val firstSyncDebrisSizeInBytes = 0L
        val secondSyncDebrisSizeInBytes = 0L
        val syncs = listOf(firstSync, secondSync)
        val expectedDebrisSize = firstSyncDebrisSizeInBytes + secondSyncDebrisSizeInBytes

        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(syncDebrisRepository.getSyncDebrisForSyncs(syncs)).thenReturn(
            listOf(
                SyncDebris(
                    firstSync.id,
                    UriPath(firstSync.localFolderPath),
                    firstSyncDebrisSizeInBytes
                ),
                SyncDebris(
                    secondSync.id,
                    UriPath(secondSync.localFolderPath),
                    secondSyncDebrisSizeInBytes
                ),
            )
        )

        val actualDebrisSize = underTest()

        assertThat(actualDebrisSize).isEqualTo(expectedDebrisSize)
    }
}
