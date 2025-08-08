package mega.privacy.android.core.nodecomponents.extension

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.icon.pack.R as IconPackR
import org.junit.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeIconExtTest {

    private val fileTypeIconMapper = FileTypeIconMapper()

    @ParameterizedTest(name = "and {0} in normal context")
    @MethodSource("provideParametersForAllDrawers")
    fun `test that folder nodes return expected icon when origin is not shares`(
        name: String,
        expectedResource: Int,
        folderNode: TypedFolderNode,
    ) {
        Truth.assertThat(
            folderNode.getIcon(
                originShares = false,
                fileTypeIconMapper = fileTypeIconMapper
            )
        ).isEqualTo(expectedResource)
    }

    @ParameterizedTest(name = "and {0} in shared context")
    @MethodSource("provideParametersForAllDrawers")
    fun `test that folder nodes return expected icon when origin is shares`(
        name: String,
        expectedResource: Int,
        folderNode: TypedFolderNode,
    ) {
        //if the node is shown from shared screen origin, outgoing share icon has priority over Camera uploads and Chat
        val outShareOverride =
            // testing a shared node
            (folderNode.isShared || folderNode.isPendingShare)
                    // tested node would be a chat or camera upload icon in another drawer
                    && (expectedResource == IconPackR.drawable.ic_folder_chat_medium_solid || expectedResource == IconPackR.drawable.ic_folder_camera_uploads_medium_solid)

        Truth.assertThat(
            folderNode.getIcon(
                originShares = false,
                fileTypeIconMapper = fileTypeIconMapper
            )
        )
            .isEqualTo(if (outShareOverride) IconPackR.drawable.ic_folder_outgoing_medium_solid else expectedResource)
    }

    private class FolderMockNameResource(
        val name: String,
        val expectedResource: Int,
        val folder: TypedFolderNode,
    ) {
        constructor(
            name: String,
            expectedResource: Int,
            stubbing: KStubbing<TypedFolderNode>.(TypedFolderNode) -> Unit,
        ) : this(name, expectedResource, mock<TypedFolderNode>(stubbing = stubbing))
    }

    private fun provideParametersForAllDrawers(): Stream<Arguments> = Stream.of(
        *expectedFolderNodeToResource.map { folderToResource ->
            Arguments.of(
                folderToResource.name,
                folderToResource.expectedResource,
                folderToResource.folder,
            )

        }.toTypedArray()
    )

    private val expectedFolderNodeToResource = listOf(
        FolderMockNameResource("InRubbishBin", IconPackR.drawable.ic_folder_medium_solid) {
            on { isInRubbishBin }.thenReturn(true)
        },
        FolderMockNameResource(
            "IncomingShare",
            IconPackR.drawable.ic_folder_incoming_medium_solid
        ) {
            on { isIncomingShare }.thenReturn(true)
        },
        FolderMockNameResource(
            "MediaSync",
            IconPackR.drawable.ic_folder_camera_uploads_medium_solid
        ) {
            on { type }.thenReturn(FolderType.MediaSyncFolder)
        },
        FolderMockNameResource("Chat", IconPackR.drawable.ic_folder_chat_medium_solid) {
            on { type }.thenReturn(FolderType.ChatFilesFolder)
        },
        FolderMockNameResource("Shared", IconPackR.drawable.ic_folder_outgoing_medium_solid) {
            on { isShared }.thenReturn(true)
        },
        FolderMockNameResource(
            "PendingShare",
            IconPackR.drawable.ic_folder_outgoing_medium_solid
        ) {
            on { isPendingShare }.thenReturn(true)
        },
        FolderMockNameResource("RootBackup", IconPackR.drawable.ic_backup_medium_solid) {
            on { type }.thenReturn(FolderType.RootBackup)
        },
        FolderMockNameResource("BackupWin", IconPackR.drawable.ic_pc_windows_medium_solid) {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Windows))
        },
        FolderMockNameResource("BackupLinux", IconPackR.drawable.ic_pc_linux_medium_solid) {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Linux))
        },
        FolderMockNameResource(
            "BackupExternal",
            IconPackR.drawable.ic_external_drive_medium_solid
        ) {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.ExternalDrive))
        },
        FolderMockNameResource("BackupUnknown", IconPackR.drawable.ic_pc_medium_solid) {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Unknown))
        },
        FolderMockNameResource(
            "BackupChild",
            IconPackR.drawable.ic_folder_backup_medium_solid
        ) {
            on { type }.thenReturn(FolderType.ChildBackup)
        },
        FolderMockNameResource(
            "Sync",
            IconPackR.drawable.ic_folder_sync_medium_solid
        ) {
            on { type }.thenReturn(FolderType.Sync)
        },
        FolderMockNameResource("Simple", IconPackR.drawable.ic_folder_medium_solid) {},
    )

    @Test
    fun `test that folders in the rubbish bin returns folder icon`() {
        val folderNode = mock<TypedFolderNode> { on { isInRubbishBin }.thenReturn(true) }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_medium_solid)
    }


    @Test
    fun `test that incoming share returns the incoming folder icon`() {
        val folderNode = mock<TypedFolderNode> { on { isIncomingShare }.thenReturn(true) }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_incoming_medium_solid)
    }

    @Test
    fun `test that media sync folders return camera backup icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.MediaSyncFolder)
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_camera_uploads_medium_solid)
    }

    @Test
    fun `test that chat files folder returns chat folder icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.ChatFilesFolder)
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_chat_medium_solid)
    }

    @Test
    fun `test that shared folder returns shared icon`() {
        val folderNode = mock<TypedFolderNode> { on { isShared }.thenReturn(true) }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_outgoing_medium_solid)
    }

    @Test
    fun `test that pending share folder returns shared icon`() {
        val folderNode = mock<TypedFolderNode> { on { isPendingShare }.thenReturn(true) }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_outgoing_medium_solid)
    }

    @Test
    fun `test that root backup folder returns cloud backup icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.RootBackup)
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_backup_medium_solid)
    }

    @Test
    fun `test that windows device backup returns windows icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Windows))
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_pc_windows_medium_solid)
    }

    @Test
    fun `test that linux device backup returns linux icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Linux))
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_pc_linux_medium_solid)
    }

    @Test
    fun `test that mac device backup returns mac icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Mac))
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_pc_mac_medium_solid)
    }

    @Test
    fun `test that external device backup returns external icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.ExternalDrive))
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_external_drive_medium_solid)
    }

    @Test
    fun `test that unknown device backup returns generic pc icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Unknown))
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_pc_medium_solid)
    }

    @Test
    fun `test that child backup folder returns backup icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.ChildBackup)
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_backup_medium_solid)
    }

    @Test
    fun `test that synced folder returns sync icon`() {
        val folderNode = mock<TypedFolderNode> {
            on { type }.thenReturn(FolderType.Sync)
        }

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_sync_medium_solid)
    }

    @Test
    fun `test that normal folder returns folder icon`() {
        val folderNode = mock<TypedFolderNode>()

        assertThat(
            getDefaultFolderIcon(
                folderNode = folderNode,
            )
        ).isEqualTo(IconPackR.drawable.ic_folder_medium_solid)
    }
}