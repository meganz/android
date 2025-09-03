package mega.privacy.android.feature.sync.data.repository

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.wrapper.DocumentFileWrapper

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.data.gateway.SyncDebrisGateway
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncDebris
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncDebrisRepositoryImplTest {

    private lateinit var underTest: SyncDebrisRepositoryImpl
    private val fileGateway: FileGateway = mock()
    private val syncDebrisGateway: SyncDebrisGateway = mock()
    private val documentFileWrapper: DocumentFileWrapper = mock()
    private val ioDispatcher = UnconfinedTestDispatcher()

    private val uriMockStatic = mockStatic(Uri::class.java)

    @AfterAll
    fun tearDownClass() {
        uriMockStatic.close()
    }

    @BeforeEach
    fun setUp() {
        underTest = SyncDebrisRepositoryImpl(
            fileGateway,
            syncDebrisGateway,
            ioDispatcher,
            documentFileWrapper
        )
    }

    @AfterEach
    fun tearDown() {
        reset(fileGateway, syncDebrisGateway, documentFileWrapper)
    }

    @Test
    fun `test that repository clears debris selectively excluding tmp folders`() = runTest {
        val debrisPath1 =
            "content://com.android.externalstorage.documents/tree/primary%3ASync%2Ffolder1%2F.debris"
        val debrisPath2 =
            "content://com.android.externalstorage.documents/tree/primary%3ASync%2Ffolder2%2F.debris"

        val debris = listOf(
            SyncDebris(123L, UriPath(debrisPath1), 1000L),
            SyncDebris(345L, UriPath(debrisPath2), 2000L)
        )

        // Mock files in debris folder 1
        val regularFile1 = mock<DocumentFile> {
            on { name } doReturn "file1.txt"
            on { isDirectory } doReturn false
            on { delete() } doReturn true
        }
        val tmpFolder1 = mock<DocumentFile> {
            on { name } doReturn "tmp"
            on { isDirectory } doReturn true
        }
        val regularFolder1 = mock<DocumentFile> {
            on { name } doReturn "regular"
            on { isDirectory } doReturn true
            on { delete() } doReturn true
        }

        // Mock files in debris folder 2
        val regularFile2 = mock<DocumentFile> {
            on { name } doReturn "file2.txt"
            on { isDirectory } doReturn false
            on { delete() } doReturn true
        }

        val documentFile1 = mock<DocumentFile> {
            on { uri } doReturn mock<Uri>()
            on { listFiles() } doReturn arrayOf(regularFile1, tmpFolder1, regularFolder1)
        }
        val documentFile2 = mock<DocumentFile> {
            on { uri } doReturn mock<Uri>()
            on { listFiles() } doReturn arrayOf(regularFile2)
        }

        whenever(syncDebrisGateway.get()).thenReturn(debris)
        whenever(documentFileWrapper.getDocumentFileForSyncContentUri(debrisPath1)).thenReturn(
            documentFile1
        )
        whenever(documentFileWrapper.getDocumentFileForSyncContentUri(debrisPath2)).thenReturn(
            documentFile2
        )

        underTest.clear()

        // Verify that regular files and folders are deleted
        verify(regularFile1).delete()
        verify(regularFolder1).delete()
        verify(regularFile2).delete()

        // Verify that tmp folder is NOT deleted
        verify(tmpFolder1, never()).delete()

        verify(syncDebrisGateway).set(emptyList())
    }

    @Test
    fun `test that repository extracts debris folder for each sync`() = runTest {
        val localFolderPath1 = "content://com.android.externalstorage.documents/tree/primary%3ASync"
        val localFolderPath2 =
            "content://com.android.externalstorage.documents/tree/primary%3AAnothersync"
        val debrisPath1 =
            "content://com.android.externalstorage.documents/tree/primary%3ASync%2F.debris"
        val debrisPath2 =
            "content://com.android.externalstorage.documents/tree/primary%3AAnothersync%2F.debris"

        val syncs = listOf(
            FolderPair(
                id = 123L,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "",
                localFolderPath = localFolderPath1,
                remoteFolder = RemoteFolder(id = NodeId(12L), name = "some folder"),
                syncStatus = SyncStatus.SYNCED
            ),
            FolderPair(
                id = 345L,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "",
                localFolderPath = localFolderPath2,
                remoteFolder = RemoteFolder(id = NodeId(434L), name = "some other folder"),
                syncStatus = SyncStatus.SYNCED
            ),
        )

        val uri1 = mock<Uri> {
            on { toString() } doReturn debrisPath1
        }
        val uri2 = mock<Uri> {
            on { toString() } doReturn debrisPath2
        }
        val debrisFolder1 = mock<DocumentFile> {
            on { uri } doReturn uri1
        }
        val debrisFolder2 = mock<DocumentFile> {
            on { uri } doReturn uri2
        }



        whenever(
            fileGateway.findFileInDirectory(UriPath(localFolderPath1), ".debris")
        ).thenReturn(debrisFolder1)
        whenever(
            fileGateway.findFileInDirectory(UriPath(localFolderPath2), ".debris")
        ).thenReturn(debrisFolder2)
        whenever(fileGateway.getTotalSizeRecursive(UriPath(debrisPath1))).thenReturn(1000L)
        whenever(fileGateway.getTotalSizeRecursive(UriPath(debrisPath2))).thenReturn(2000L)

        val expected = listOf(
            SyncDebris(
                syncId = 123L,
                path = UriPath(debrisPath1),
                sizeInBytes = 1000L
            ),
            SyncDebris(
                syncId = 345L,
                path = UriPath(debrisPath2),
                sizeInBytes = 2000L
            )
        )

        val actual = underTest.getSyncDebrisForSyncs(syncs)

        assertThat(actual).isEqualTo(expected)
        verify(syncDebrisGateway).set(expected)
    }

    @Test
    fun `test that clear method excludes tmp folders from deletion`() = runTest {
        val debrisPath1 =
            "content://com.android.externalstorage.documents/tree/primary%3ASync%2Ffolder1%2F.debris"
        val debrisPath2 =
            "content://com.android.externalstorage.documents/tree/primary%3ASync%2Ffolder2%2F.debris"

        val debris = listOf(
            SyncDebris(123L, UriPath(debrisPath1), 1000L),
            SyncDebris(345L, UriPath(debrisPath2), 2000L)
        )

        // Mock files in debris folder 1
        val regularFile1 = mock<DocumentFile> {
            on { name } doReturn "file1.txt"
            on { isDirectory } doReturn false
            on { delete() } doReturn true
        }
        val tmpFolder1 = mock<DocumentFile> {
            on { name } doReturn "tmp"
            on { isDirectory } doReturn true
        }
        val regularFolder1 = mock<DocumentFile> {
            on { name } doReturn "regular"
            on { isDirectory } doReturn true
            on { delete() } doReturn true
        }

        // Mock files in debris folder 2
        val regularFile2 = mock<DocumentFile> {
            on { name } doReturn "file2.txt"
            on { isDirectory } doReturn false
            on { delete() } doReturn true
        }

        val documentFile1 = mock<DocumentFile> {
            on { uri } doReturn mock<Uri>()
            on { listFiles() } doReturn arrayOf(regularFile1, tmpFolder1, regularFolder1)
        }
        val documentFile2 = mock<DocumentFile> {
            on { uri } doReturn mock<Uri>()
            on { listFiles() } doReturn arrayOf(regularFile2)
        }

        whenever(syncDebrisGateway.get()).thenReturn(debris)
        whenever(documentFileWrapper.getDocumentFileForSyncContentUri(debrisPath1)).thenReturn(
            documentFile1
        )
        whenever(documentFileWrapper.getDocumentFileForSyncContentUri(debrisPath2)).thenReturn(
            documentFile2
        )

        underTest.clear()

        // Verify that regular files and folders are deleted
        verify(regularFile1).delete()
        verify(regularFolder1).delete()
        verify(regularFile2).delete()

        // Verify that tmp folder is NOT deleted
        verify(tmpFolder1, never()).delete()

        verify(syncDebrisGateway).set(emptyList())
    }
}
