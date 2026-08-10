package mega.privacy.android.feature.sync.domain.usecase.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetLocalSyncOrBackupUriPathUseCaseImplTest {

    private lateinit var underTest: GetLocalSyncOrBackupUriPathUseCaseImpl

    private val getFolderPairsUseCase = mock<GetFolderPairsUseCase>()

    @BeforeAll
    fun setup() {
        underTest = GetLocalSyncOrBackupUriPathUseCaseImpl(
            getFolderPairsUseCase = getFolderPairsUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getFolderPairsUseCase)
    }

    @Test
    fun `test that empty list is returned when no folder pairs exist`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(emptyList())

        val result = underTest.invoke()

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that single uri path is returned when one folder pair exists`() = runTest {
        val localPath = "/storage/emulated/0/Documents"
        val folderPair = createFolderPair(localFolderPath = localPath)
        whenever(getFolderPairsUseCase()).thenReturn(listOf(folderPair))

        val result = underTest.invoke()

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(UriPath(localPath))
    }

    @Test
    fun `test that multiple uri paths are returned when multiple folder pairs exist`() = runTest {
        val localPath1 = "/storage/emulated/0/Documents"
        val localPath2 = "/storage/emulated/0/Pictures"
        val localPath3 = "content://com.android.providers.media.documents/tree/primary%3ADCIM"

        val folderPairs = listOf(
            createFolderPair(id = 1, localFolderPath = localPath1),
            createFolderPair(id = 2, localFolderPath = localPath2),
            createFolderPair(id = 3, localFolderPath = localPath3)
        )
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)

        val result = underTest.invoke()

        assertThat(result).containsExactly(
            UriPath(localPath1),
            UriPath(localPath2),
            UriPath(localPath3)
        ).inOrder()
    }

    @Test
    fun `test that uri paths with content uri are handled correctly`() = runTest {
        val contentUri = "content://com.android.providers.media.documents/tree/primary%3ADCIM"
        val folderPair = createFolderPair(localFolderPath = contentUri)
        whenever(getFolderPairsUseCase()).thenReturn(listOf(folderPair))

        val result = underTest.invoke()

        assertThat(result[0]).isEqualTo(UriPath(contentUri))
    }

    @Test
    fun `test that uri paths with file paths are handled correctly`() = runTest {
        val filePath = "/storage/emulated/0/MEGA/Camera Uploads"
        val folderPair = createFolderPair(localFolderPath = filePath)
        whenever(getFolderPairsUseCase()).thenReturn(listOf(folderPair))

        val result = underTest.invoke()

        assertThat(result[0]).isEqualTo(UriPath(filePath))
    }

    private fun createFolderPair(
        id: Long = 1L,
        syncType: SyncType = SyncType.TYPE_TWOWAY,
        pairName: String = "Test Sync",
        localFolderPath: String = "/storage/emulated/0/Documents",
        remoteFolder: RemoteFolder = RemoteFolder(NodeId(123L), "Remote Test"),
        syncStatus: SyncStatus = SyncStatus.SYNCED,
    ) = FolderPair(
        id = id,
        syncType = syncType,
        pairName = pairName,
        localFolderPath = localFolderPath,
        remoteFolder = remoteFolder,
        syncStatus = syncStatus
    )
}
