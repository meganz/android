package mega.privacy.android.app.presentation.fileinfo.model

import mega.privacy.android.icon.pack.R as IconPackR
import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.fileinfo.model.getNodeIcon
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.mock
import java.util.stream.Stream

class GetFolderIconTest {
    private val fileTypeIconMapper = FileTypeIconMapper()

    @ParameterizedTest(name = "test that folder {0} not in shared items has the expected icon")
    @MethodSource("provideParametersForAllDrawers")
    fun `test that folder nodes return expected icon when origin is not shares`(
        name: String,
        expectedResource: Int,
        folderNode: TypedFolderNode,
    ) {

        Truth.assertThat(
            getNodeIcon(
                typedNode = folderNode,
                originShares = false,
                fileTypeIconMapper = fileTypeIconMapper
            )
        ).isEqualTo(expectedResource)
    }

    @ParameterizedTest(name = "test that folder {0} in shared items has the expected icon")
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
            getNodeIcon(
                typedNode = folderNode,
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

    companion object {

        @JvmStatic
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
            FolderMockNameResource("Simple", IconPackR.drawable.ic_folder_medium_solid) {},
        )
    }
}