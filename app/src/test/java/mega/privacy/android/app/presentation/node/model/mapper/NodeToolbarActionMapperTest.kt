package mega.privacy.android.app.presentation.node.model.mapper

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.mapper.NodeToolbarActionMapper
import mega.privacy.android.app.presentation.node.model.menuaction.ClearSelectionMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.CopyMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.DisputeTakeDownMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.DownloadMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.GetLinkMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.LeaveShareMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.MoveMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.RemoveMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.RestoreMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.SelectAllMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.SendToChatMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.ShareFolderMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.ShareMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.TrashMenuAction
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ClearSelectionToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.CopyToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.DisputeTakeDownMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.DownloadToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.GetLinkToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.LeaveShareToolBarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ManageLinkToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.MoveToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.MultiSelectManageLinkToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveLinkToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveShareDropDown
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveShareToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RenameDropdownMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RenameToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RestoreToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.SelectAllToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.SendToChatToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ShareFolderToolbarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ShareToolBarMenuItem
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.TrashToolbarMenuItem
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class NodeToolbarActionMapperTest {
    val underTest = NodeToolbarActionMapper()
    private val toolbarList = setOf(
        SelectAllToolbarMenuItem(SelectAllMenuAction()),
        ClearSelectionToolbarMenuItem(ClearSelectionMenuAction()),
        DownloadToolbarMenuItem(DownloadMenuAction()),
        RestoreToolbarMenuItem(
            menuAction = RestoreMenuAction(),
            checkNodesNameCollisionUseCase = mock(),
            restoreNodesUseCase = mock(),
            restoreNodeResultMapper = mock(),
            snackBarHandler = mock()
        ),
        RemoveToolbarMenuItem(RemoveMenuAction(), mock()),
        RemoveShareToolbarMenuItem(
            stringWithDelimitersMapper = mock()
        ),
        RemoveShareDropDown(mock()),
        DisputeTakeDownMenuItem(DisputeTakeDownMenuAction()),
        GetLinkToolbarMenuItem(GetLinkMenuAction()),
        ManageLinkToolbarMenuItem(),
        MultiSelectManageLinkToolbarMenuItem(),
        RemoveLinkToolbarMenuItem(mock()),
        SendToChatToolbarMenuItem(
            menuAction = SendToChatMenuAction(),
            getNodeToAttachUseCase = mock(),
        ),
        ShareFolderToolbarMenuItem(
            checkBackupNodeTypeByHandleUseCase = mock(),
            listToStringWithDelimitersMapper = mock(),
            menuAction = ShareFolderMenuAction()
        ),
        ShareToolBarMenuItem(
            menuAction = ShareMenuAction(),
            exportNodesUseCase = mock(),
            getFileUriUseCase = mock(),
            getLocalFilePathUseCase = mock()
        ),
        LeaveShareToolBarMenuItem(
            stringWithDelimitersMapper = mock(),
            menuAction = LeaveShareMenuAction()
        ),
        RenameToolbarMenuItem(),
        RenameDropdownMenuItem(),
        CopyToolbarMenuItem(CopyMenuAction()),
        MoveToolbarMenuItem(MoveMenuAction()),
        TrashToolbarMenuItem(TrashMenuAction(), mock()),
    )

    @Test
    fun `test that node toolbar mapper returns list of menu action`() = runTest {
        val mappedOptions = underTest.invoke(
            toolbarOptions = toolbarList,
            hasNodeAccessPermission = true,
            selectedNodes = listOf(mock<TypedFileNode>()),
            allNodeCanBeMovedToTarget = true,
            noNodeInBackups = true,
            resultCount = 10,
        )
        Truth.assertThat(mappedOptions.first().action).isInstanceOf(MenuAction::class.java)
    }

    @Test
    fun `test that any selected node is a folder then send to chat option will not be shown to the user`() =
        runTest {
            val mappedOptions = underTest.invoke(
                toolbarOptions = toolbarList,
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mock<TypedFolderNode>()),
                allNodeCanBeMovedToTarget = true,
                noNodeInBackups = true,
                resultCount = 10,
            )
            Truth.assertThat(mappedOptions).doesNotContain(SendToChatMenuAction())
        }

    @Test
    fun `test that any selected node is taken down then send to chat option will not be shown to the user`() =
        runTest {
            val mappedOptions = underTest.invoke(
                toolbarOptions = toolbarList,
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mock<TypedFolderNode> {
                    on { isTakenDown }.thenReturn(true)
                }),
                allNodeCanBeMovedToTarget = true,
                noNodeInBackups = true,
                resultCount = 10,
            )
            Truth.assertThat(mappedOptions)
                .doesNotContain(GetLinkToolbarMenuItem(GetLinkMenuAction()))
        }
}