package test.mega.privacy.android.app.presentation.fileinfo.model

import com.google.common.truth.Truth
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.fileinfo.model.getNodeIcon
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.mock
import java.util.stream.Stream

class GetFolderIconTest {
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
                    && (expectedResource == R.drawable.ic_folder_chat_list || expectedResource == R.drawable.ic_folder_camera_uploads_list)

        Truth.assertThat(
            getNodeIcon(
                typedNode = folderNode,
                originShares = false,
            )
        ).isEqualTo(if (outShareOverride) R.drawable.ic_folder_outgoing else expectedResource)
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
            FolderMockNameResource("InRubbishBin", R.drawable.ic_folder_list) {
                on { isInRubbishBin }.thenReturn(true)
            },
            FolderMockNameResource("IncomingShare", R.drawable.ic_folder_incoming) {
                on { isIncomingShare }.thenReturn(true)
            },
            FolderMockNameResource("MediaSync", R.drawable.ic_folder_camera_uploads_list) {
                on { type }.thenReturn(FolderType.MediaSyncFolder)
            },
            FolderMockNameResource("Chat", R.drawable.ic_folder_chat_list) {
                on { type }.thenReturn(FolderType.ChatFilesFolder)
            },
            FolderMockNameResource("Shared", R.drawable.ic_folder_outgoing) {
                on { isShared }.thenReturn(true)
            },
            FolderMockNameResource("PendingShare", R.drawable.ic_folder_outgoing) {
                on { isPendingShare }.thenReturn(true)
            },
            FolderMockNameResource("RootBackup", R.drawable.backup) {
                on { type }.thenReturn(FolderType.RootBackup)
            },
            FolderMockNameResource("BackupWin", R.drawable.pc_win) {
                on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Windows))
            },
            FolderMockNameResource("BackupLinux", R.drawable.pc_linux) {
                on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Linux))
            },
            FolderMockNameResource("BackupExternal", R.drawable.ex_drive) {
                on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.ExternalDrive))
            },
            FolderMockNameResource("BackupUnknown", R.drawable.pc) {
                on { type }.thenReturn(FolderType.DeviceBackup(DeviceType.Unknown))
            },
            FolderMockNameResource("BackupChild", R.drawable.ic_folder_backup) {
                on { type }.thenReturn(FolderType.ChildBackup)
            },
            FolderMockNameResource("Simple", R.drawable.ic_folder_list) {},
        )
    }
}