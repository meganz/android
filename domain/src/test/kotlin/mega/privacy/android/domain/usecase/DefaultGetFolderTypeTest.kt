package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetFolderTypeTest {
    private lateinit var underTest: GetFolderType

    private val folderId = NodeId(1234L)
    private val parentId = NodeId(76543L)
    private val testFolder = mock<FolderNode> {
        on { id }.thenReturn(folderId)
        on { parentId }.thenReturn(parentId)
    }


    private val chatRepository = mock<ChatRepository>()
    private val backupFolderId = Result.success(NodeId(folderId.id + 1))

    private val monitorBackupFolder = mock<MonitorBackupFolder> {
        onBlocking { invoke() }.thenReturn(flow {
            emit(backupFolderId)
            awaitCancellation()
        }
        )
    }
    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val hasAncestor = mock<HasAncestor> {
        onBlocking { invoke(folderId, backupFolderId.getOrThrow()) }.thenReturn(false)
    }
    private val getDeviceType =
        mock<GetDeviceType> { onBlocking { invoke(any()) }.thenReturn(DeviceType.Unknown) }

    @Before
    fun setUp() {
        underTest = DefaultGetFolderType(
            cameraUploadRepository = cameraUploadRepository,
            chatRepository = chatRepository,
            monitorBackupFolder = monitorBackupFolder,
            hasAncestor = hasAncestor,
            getDeviceType = getDeviceType,
        )
    }

    @Test
    fun `test that normal folders are marked as default`() = runTest {
        val actual = underTest(testFolder)

        assertThat(actual).isEqualTo(FolderType.Default)
    }

    @Test
    fun `test that the primary sync folder is marked as a mediaFolder`() = runTest {
        cameraUploadRepository.stub {
            onBlocking { getPrimarySyncHandle() }.thenReturn(folderId.id)
        }
        val actual = underTest(testFolder)

        assertThat(actual).isEqualTo(FolderType.MediaSyncFolder)
    }

    @Test
    fun `tet that the secondary sync folder is marked as a media folder`() = runTest {
        cameraUploadRepository.stub {
            onBlocking { getSecondarySyncHandle() }.thenReturn(folderId.id)
        }
        val actual = underTest(testFolder)

        assertThat(actual).isEqualTo(FolderType.MediaSyncFolder)
    }

    @Test
    fun `test that chat files folder is marked as chat files folder`() = runTest {
        chatRepository.stub {
            onBlocking { getChatFilesFolderId() }.thenReturn(folderId)
        }
        val actual = underTest(testFolder)

        assertThat(actual).isEqualTo(FolderType.ChatFilesFolder)
    }

    @Test
    fun `test that backup folder is marked as root backup type`() = runTest {
        monitorBackupFolder.stub {
            onBlocking { invoke() }.thenReturn(flow {
                emit(Result.success(folderId))
                awaitCancellation()
            }
            )
        }

        val actual = underTest(testFolder)

        assertThat(actual).isEqualTo(FolderType.RootBackup)
    }

    @Test
    fun `test that a child of the backup folder is marked as child backup type`() = runTest {
        hasAncestor.stub {
            onBlocking { invoke(folderId, backupFolderId.getOrThrow()) }.thenReturn(true)
        }

        val actual = underTest(testFolder)

        assertThat(actual).isEqualTo(FolderType.ChildBackup)
    }

    @Test
    fun `test that node with a device id is marked as device folder`() =
        runTest {
            testFolder.stub {
                on { device }.thenReturn("deviceId")
            }
            val actual = underTest(testFolder)

            assertThat(actual).isInstanceOf(FolderType.DeviceBackup::class.java)
        }

    @Test
    fun `test that device folders match returned device type`() = runTest {
        val expected = DeviceType.Mac
        testFolder.stub {
            on { device }.thenReturn("deviceId")
        }
        getDeviceType.stub {
            onBlocking { invoke(any()) }.thenReturn(expected)
        }

        val actual = underTest(testFolder)

        assertThat(actual).isEqualTo(FolderType.DeviceBackup(expected))
    }

    @Test
    fun `test that default folder with failed backup folder result still returns default folder result`() =
        runTest {
            monitorBackupFolder.stub {
                onBlocking { invoke() }.thenReturn(flow {
                    emit(Result.failure(Throwable()))
                    awaitCancellation()
                }
                )
            }

            val actual = underTest(testFolder)

            assertThat(actual).isEqualTo(FolderType.Default)
        }

}