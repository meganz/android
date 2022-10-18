package test.mega.privacy.android.app.presentation.favourites.model.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.favourites.model.mapper.getFolderIcon
import mega.privacy.android.domain.entity.BackupType
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderNode
import org.junit.Test
import org.mockito.kotlin.mock

class GetFolderIconTest {
    @Test
    fun `test that folders in the rubbish bin returns folder icon`() {
        val folder = mock<FolderNode> { on { isInRubbishBin }.thenReturn(true) }

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = false,
                backupType = BackupType.None,
            )
        ).isEqualTo(R.drawable.ic_folder_list)
    }

    @Test
    fun `test that incoming share returns the incoming folder icon`() {
        val folder = mock<FolderNode> { on { isIncomingShare }.thenReturn(true) }

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = false,
                backupType = BackupType.None,
            )
        ).isEqualTo(R.drawable.ic_folder_incoming)
    }

    @Test
    fun `test that camera backup folders return camera backup icon`() {
        val folder = mock<FolderNode>()

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = true,
                isChatFilesFolder = false,
                backupType = BackupType.None,
            )
        ).isEqualTo(R.drawable.ic_folder_camera_uploads_list)
    }

    @Test
    fun `test that chat files folder returns chat folder icon`() {
        val folder = mock<FolderNode>()

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = true,
                backupType = BackupType.None,
            )
        ).isEqualTo(R.drawable.ic_folder_chat_list)
    }

    @Test
    fun `test that shared folder returns shared icon`() {
        val folder = mock<FolderNode> { on { isShared }.thenReturn(true) }

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = false,
                backupType = BackupType.None,
            )
        ).isEqualTo(R.drawable.ic_folder_outgoing)
    }

    @Test
    fun `test that pending share folder returns shared icon`() {
        val folder = mock<FolderNode> { on { isPendingShare }.thenReturn(true) }

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = false,
                backupType = BackupType.None,
            )
        ).isEqualTo(R.drawable.ic_folder_outgoing)
    }

    @Test
    fun `test that root backup folder returns cloud backup icon`() {
        val folder = mock<FolderNode>()

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = false,
                backupType = BackupType.Root,
            )
        ).isEqualTo(R.drawable.backup)
    }

    @Test
    fun `test that windows device backup returns windows icon`() {
        val folder = mock<FolderNode>()

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = false,
                backupType = BackupType.Device(DeviceType.Windows),
            )
        ).isEqualTo(R.drawable.pc_win)
    }

    @Test
    fun `test that linux device backup returns linux icon`() {
        val folder = mock<FolderNode>()

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = false,
                backupType = BackupType.Device(DeviceType.Linux),
            )
        ).isEqualTo(R.drawable.pc_linux)
    }

    @Test
    fun `test that mac device backup returns mac icon`() {
        val folder = mock<FolderNode>()

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = false,
                backupType = BackupType.Device(DeviceType.Mac),
            )
        ).isEqualTo(R.drawable.pc_mac)
    }

    @Test
    fun `test that external device backup returns external icon`() {
        val folder = mock<FolderNode>()

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = false,
                backupType = BackupType.Device(DeviceType.ExternalDrive),
            )
        ).isEqualTo(R.drawable.ex_drive)
    }

    @Test
    fun `test that unknown device backup returns generic pc icon`() {
        val folder = mock<FolderNode>()

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = false,
                backupType = BackupType.Device(DeviceType.Unknown),
            )
        ).isEqualTo(R.drawable.pc)
    }

    @Test
    fun `test that child backup folder returns backup icon`() {
        val folder = mock<FolderNode>()

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = false,
                backupType = BackupType.Child,
            )
        ).isEqualTo(R.drawable.ic_folder_backup)
    }

    @Test
    fun `test that normal folder returns folder icon`() {
        val folder = mock<FolderNode>()

        assertThat(
            getFolderIcon(
                folder = folder,
                isMediaSyncFolder = false,
                isChatFilesFolder = false,
                backupType = BackupType.None,
            )
        ).isEqualTo(R.drawable.ic_folder_list)
    }

}