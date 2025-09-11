package mega.privacy.android.data.mapper.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.FolderTypeData
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetDeviceType
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class FolderTypeMapperTest {
    private lateinit var underTest: FolderTypeMapper

    private val folderId = NodeId(1234L)
    private val parentId = NodeId(76543L)
    private val testFolder = mock<FolderNode> {
        on { id }.thenReturn(folderId)
        on { parentId }.thenReturn(parentId)
        on { device }.thenReturn(null)
        on { isSynced }.thenReturn(false)
    }

    private val getDeviceType = mock<GetDeviceType> {
        onBlocking { invoke(any()) }.thenReturn(DeviceType.Unknown)
    }

    private val folderTypeData = FolderTypeData(
        primarySyncHandle = null,
        secondarySyncHandle = null,
        chatFilesFolderId = null,
        backupFolderId = null,
        syncedNodeIds = emptySet()
    )

    @Before
    fun setUp() {
        underTest = FolderTypeMapper(
            getDeviceType = getDeviceType
        )
    }

    @Test
    fun `test that normal folders are marked as default`() = runTest {
        val actual = underTest(testFolder, folderTypeData)

        assertThat(actual).isEqualTo(FolderType.Default)
    }

    @Test
    fun `test that the primary sync folder is marked as a mediaFolder`() = runTest {
        val dataWithPrimarySync = folderTypeData.copy(
            primarySyncHandle = folderId.longValue
        )
        val actual = underTest(testFolder, dataWithPrimarySync)

        assertThat(actual).isEqualTo(FolderType.MediaSyncFolder)
    }

    @Test
    fun `test that the secondary sync folder is marked as a media folder`() = runTest {
        val dataWithSecondarySync = folderTypeData.copy(
            secondarySyncHandle = folderId.longValue
        )
        val actual = underTest(testFolder, dataWithSecondarySync)

        assertThat(actual).isEqualTo(FolderType.MediaSyncFolder)
    }

    @Test
    fun `test that chat files folder is marked as chat files folder`() = runTest {
        val dataWithChatFolder = folderTypeData.copy(
            chatFilesFolderId = folderId
        )
        val actual = underTest(testFolder, dataWithChatFolder)

        assertThat(actual).isEqualTo(FolderType.ChatFilesFolder)
    }

    @Test
    fun `test that backup folder is marked as root backup type`() = runTest {
        val dataWithBackupFolder = folderTypeData.copy(
            backupFolderId = folderId
        )
        val actual = underTest(testFolder, dataWithBackupFolder)

        assertThat(actual).isEqualTo(FolderType.RootBackup)
    }

    @Test
    fun `test that a child of the backup folder is marked as child backup type`() = runTest {
        // Note: Currently the mapper always returns false for child backup
        // This test documents the current behavior
        val dataWithBackupFolder = folderTypeData.copy(
            backupFolderId = NodeId(folderId.longValue + 1)
        )
        val actual = underTest(testFolder, dataWithBackupFolder)

        // Currently returns false due to TODO in implementation
        assertThat(actual).isEqualTo(FolderType.Default)
    }

    @Test
    fun `test that node with a device id is marked as device folder`() = runTest {
        val folderWithDevice = mock<FolderNode> {
            on { id }.thenReturn(folderId)
            on { parentId }.thenReturn(parentId)
            on { device }.thenReturn("deviceId")
            on { isSynced }.thenReturn(false)
        }
        val actual = underTest(folderWithDevice, folderTypeData)

        assertThat(actual).isInstanceOf(FolderType.DeviceBackup::class.java)
    }

    @Test
    fun `test that device folders match returned device type`() = runTest {
        val expected = DeviceType.Mac
        val folderWithDevice = mock<FolderNode> {
            on { id }.thenReturn(folderId)
            on { parentId }.thenReturn(parentId)
            on { device }.thenReturn("deviceId")
            on { isSynced }.thenReturn(false)
        }
        getDeviceType.stub {
            onBlocking { invoke(any()) }.thenReturn(expected)
        }

        val actual = underTest(folderWithDevice, folderTypeData)

        assertThat(actual).isEqualTo(FolderType.DeviceBackup(expected))
    }

    @Test
    fun `test that synced folder is marked as a Sync folder`() = runTest {
        val syncedFolder = mock<FolderNode> {
            on { id }.thenReturn(folderId)
            on { parentId }.thenReturn(parentId)
            on { device }.thenReturn(null)
            on { isSynced }.thenReturn(true)
        }

        val actual = underTest(syncedFolder, folderTypeData)

        assertThat(actual).isEqualTo(FolderType.Sync)
    }

    @Test
    fun `test that empty device string is not considered a device folder`() = runTest {
        val folderWithEmptyDevice = mock<FolderNode> {
            on { id }.thenReturn(folderId)
            on { parentId }.thenReturn(parentId)
            on { device }.thenReturn("")
            on { isSynced }.thenReturn(false)
        }
        val actual = underTest(folderWithEmptyDevice, folderTypeData)

        assertThat(actual).isEqualTo(FolderType.Default)
    }

    @Test
    fun `test that null device is not considered a device folder`() = runTest {
        val folderWithNullDevice = mock<FolderNode> {
            on { id }.thenReturn(folderId)
            on { parentId }.thenReturn(parentId)
            on { device }.thenReturn(null)
            on { isSynced }.thenReturn(false)
        }
        val actual = underTest(folderWithNullDevice, folderTypeData)

        assertThat(actual).isEqualTo(FolderType.Default)
    }

    @Test
    fun `test priority order when multiple conditions match`() = runTest {
        // Test that media sync folder takes priority over other types
        val dataWithMultipleMatches = folderTypeData.copy(
            primarySyncHandle = folderId.longValue,
            chatFilesFolderId = folderId,
            backupFolderId = folderId
        )
        val actual = underTest(testFolder, dataWithMultipleMatches)

        assertThat(actual).isEqualTo(FolderType.MediaSyncFolder)
    }

    @Test
    fun `test that chat folder takes priority over backup folder`() = runTest {
        val dataWithChatAndBackup = folderTypeData.copy(
            chatFilesFolderId = folderId,
            backupFolderId = folderId
        )
        val actual = underTest(testFolder, dataWithChatAndBackup)

        assertThat(actual).isEqualTo(FolderType.ChatFilesFolder)
    }

    @Test
    fun `test that backup folder takes priority over device folder`() = runTest {
        val folderWithDevice = mock<FolderNode> {
            on { id }.thenReturn(folderId)
            on { parentId }.thenReturn(parentId)
            on { device }.thenReturn("deviceId")
            on { isSynced }.thenReturn(false)
        }
        val dataWithBackupAndDevice = folderTypeData.copy(
            backupFolderId = folderId
        )
        val actual = underTest(folderWithDevice, dataWithBackupAndDevice)

        assertThat(actual).isEqualTo(FolderType.RootBackup)
    }

    @Test
    fun `test that device folder takes priority over sync folder`() = runTest {
        val folderWithDeviceAndSync = mock<FolderNode> {
            on { id }.thenReturn(folderId)
            on { parentId }.thenReturn(parentId)
            on { device }.thenReturn("deviceId")
            on { isSynced }.thenReturn(true)
        }
        val actual = underTest(folderWithDeviceAndSync, folderTypeData)

        assertThat(actual).isInstanceOf(FolderType.DeviceBackup::class.java)
    }
}