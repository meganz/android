package mega.privacy.android.feature.sync.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.feature.sync.data.gateway.SyncDebrisGateway
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncDebris
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncDebrisRepositoryImplTest {

    private lateinit var underTest: SyncDebrisRepositoryImpl
    private val fileGateway: FileGateway = mock()
    private val syncDebrisGateway: SyncDebrisGateway = mock()
    private val ioDispatcher = UnconfinedTestDispatcher()

    @TempDir
    lateinit var temporaryFolderOne: File

    @TempDir
    lateinit var temporaryFolderTwo: File

    @BeforeEach
    fun setUp() {
        underTest = SyncDebrisRepositoryImpl(
            fileGateway,
            syncDebrisGateway,
            ioDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        reset(fileGateway)
    }

    @Test
    fun `test that repository deletes all debris`() = runTest {
        val debris = listOf(
            SyncDebris(123L, "path1", 1000L),
            SyncDebris(345L, "path2", 2000L)
        )
        whenever(syncDebrisGateway.get()).thenReturn(debris)

        underTest.clear()

        verify(fileGateway).deleteDirectory("path1")
        verify(fileGateway).deleteDirectory("path2")
        verify(syncDebrisGateway).set(emptyList())
    }

    @Test
    fun `test that repository extracts debris folder for each sync`() = runTest {
        val syncs = listOf(
            FolderPair(
                123L,
                "",
                "storage/emulated/0/sync",
                RemoteFolder(12L, "some folder"),
                SyncStatus.SYNCED
            ),
            FolderPair(
                345L,
                "",
                "storage/emulated/0/anothersync",
                RemoteFolder(434L, "some other folder"),
                SyncStatus.SYNCED
            ),
        )
        whenever(
            fileGateway.findFileInDirectory(syncs[0].localFolderPath, ".debris")
        ).thenReturn(temporaryFolderOne)
        whenever(
            fileGateway.findFileInDirectory(syncs[1].localFolderPath, ".debris")
        ).thenReturn(temporaryFolderTwo)
        whenever(fileGateway.getTotalSize(temporaryFolderOne)).thenReturn(1000L)
        whenever(fileGateway.getTotalSize(temporaryFolderTwo)).thenReturn(2000L)
        val expected = listOf(
            SyncDebris(
                syncId = 123L, path = temporaryFolderOne.absolutePath, sizeInBytes = 1000L
            ), SyncDebris(
                syncId = 345L, path = temporaryFolderTwo.absolutePath, sizeInBytes = 2000L
            )
        )

        val actual = underTest.getSyncDebrisForSyncs(syncs)

        assertThat(actual).isEqualTo(expected)
        verify(syncDebrisGateway).set(expected)
    }
}