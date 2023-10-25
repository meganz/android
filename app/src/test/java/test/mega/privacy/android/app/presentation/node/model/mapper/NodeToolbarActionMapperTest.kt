package test.mega.privacy.android.app.presentation.node.model.mapper

import com.google.common.truth.Truth
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
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ClearSelection
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Copy
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.DisputeTakeDown
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Download
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.GetLink
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.LeaveShare
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ManageLink
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Move
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.MultiSelectManageLink
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Remove
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveLink
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveLinkDropDown
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveShare
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RemoveShareDropDown
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Rename
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.RenameDropDown
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Restore
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.SelectAll
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.SendToChat
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Share
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ShareFolder
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.Trash
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class NodeToolbarActionMapperTest {
    val underTest = NodeToolbarActionMapper()
    private val toolbarList = setOf(
        SelectAll(SelectAllMenuAction()),
        ClearSelection(ClearSelectionMenuAction()),
        Download(DownloadMenuAction()),
        Restore(RestoreMenuAction()),
        Remove(RemoveMenuAction()),
        RemoveShare(),
        RemoveShareDropDown(),
        DisputeTakeDown(DisputeTakeDownMenuAction()),
        GetLink(GetLinkMenuAction()),
        ManageLink(),
        MultiSelectManageLink(),
        RemoveLink(),
        RemoveLinkDropDown(),
        SendToChat(SendToChatMenuAction()),
        ShareFolder(ShareFolderMenuAction()),
        Share(ShareMenuAction()),
        LeaveShare(LeaveShareMenuAction()),
        Rename(),
        RenameDropDown(),
        Move(MoveMenuAction()),
        Copy(CopyMenuAction()),
        Trash(TrashMenuAction()),
    )

    @Test
    fun `test that node toolbar mapper returns list of menu action`() {
        val mappedOptions = underTest.invoke(
            toolbarOptions = toolbarList,
            hasNodeAccessPermission = true,
            selectedNodes = setOf(mock<TypedFileNode>()),
            allNodeCanBeMovedToTarget = true,
            noNodeInBackups = true,
            resultCount = 10,
        )
        Truth.assertThat(mappedOptions.first()).isInstanceOf(MenuAction::class.java)
    }

    @Test
    fun `test that any selected node is a folder then send to chat option will not be shown to the user`() {
        val mappedOptions = underTest.invoke(
            toolbarOptions = toolbarList,
            hasNodeAccessPermission = true,
            selectedNodes = setOf(mock<TypedFolderNode>()),
            allNodeCanBeMovedToTarget = true,
            noNodeInBackups = true,
            resultCount = 10,
        )
        Truth.assertThat(mappedOptions).doesNotContain(SendToChatMenuAction())
    }

    @Test
    fun `test that any selected node is taken down then send to chat option will not be shown to the user`() {
        val mappedOptions = underTest.invoke(
            toolbarOptions = toolbarList,
            hasNodeAccessPermission = true,
            selectedNodes = setOf(mock<TypedFolderNode> {
                on { isTakenDown }.thenReturn(true)
            }),
            allNodeCanBeMovedToTarget = true,
            noNodeInBackups = true,
            resultCount = 10,
        )
        Truth.assertThat(mappedOptions).doesNotContain(GetLink(GetLinkMenuAction()))
    }
}