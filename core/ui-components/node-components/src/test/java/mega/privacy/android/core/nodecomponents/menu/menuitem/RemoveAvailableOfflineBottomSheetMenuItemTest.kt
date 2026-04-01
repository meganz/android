package mega.privacy.android.core.nodecomponents.menu.menuitem

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveOfflineMenuAction
import mega.privacy.android.core.nodecomponents.model.OfflineTypedFileNode
import mega.privacy.android.core.nodecomponents.model.OfflineTypedFolderNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.usecase.foldernode.IsFolderEmptyUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveAvailableOfflineBottomSheetMenuItemTest {

    private val isFolderEmptyUseCase = mock<IsFolderEmptyUseCase>()
    private val underTest = RemoveAvailableOfflineBottomSheetMenuItem(
        menuAction = mock<RemoveOfflineMenuAction>(),
        isFolderEmptyUseCase = isFolderEmptyUseCase,
    )

    @Test
    fun `test that shouldDisplay returns true when node is available offline and not in rubbish`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { isAvailableOffline } doReturn true
                on { isTakenDown } doReturn false
            }
            whenever(isFolderEmptyUseCase(node)).thenReturn(false)
            val result = underTest.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = null,
                isInBackups = false,
                node = node,
                isConnected = true,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )
            assertThat(result).isTrue()
        }

    @Test
    fun `test that shouldDisplay returns false when node is not available offline`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn false
            on { isTakenDown } doReturn false
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is in rubbish`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn true
            on { isTakenDown } doReturn false
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = true,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when node is taken down`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn true
            on { isTakenDown } doReturn true
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(false)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns false when folder is empty`() = runTest {
        val node = mock<TypedFileNode> {
            on { isAvailableOffline } doReturn true
            on { isTakenDown } doReturn false
        }
        whenever(isFolderEmptyUseCase(node)).thenReturn(true)
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns true for OfflineTypedFileNode without checking isFolderEmptyUseCase`() =
        runTest {
            val offlineInfo = OfflineFileInformation(
                nodeInfo = OtherOfflineNodeInformation(
                    id = 1,
                    path = "/offline/file.pdf",
                    name = "file.pdf",
                    handle = "123456789",
                    isFolder = false,
                    lastModifiedTime = 1000L,
                    parentId = -1
                )
            )
            val node = OfflineTypedFileNode.from(offlineInfo)
            val result = underTest.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = null,
                isInBackups = false,
                node = node,
                isConnected = true,
                nodeSourceType = NodeSourceType.OFFLINE
            )
            assertThat(result).isTrue()
        }

    @Test
    fun `test that shouldDisplay returns true for OfflineTypedFolderNode without checking isFolderEmptyUseCase`() =
        runTest {
            val offlineInfo = OfflineFileInformation(
                nodeInfo = OtherOfflineNodeInformation(
                    id = 1,
                    path = "/offline/folder/",
                    name = "folder",
                    handle = "123456789",
                    isFolder = true,
                    lastModifiedTime = 1000L,
                    parentId = -1
                )
            )
            val node = OfflineTypedFolderNode.from(offlineInfo)
            val result = underTest.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = null,
                isInBackups = false,
                node = node,
                isConnected = true,
                nodeSourceType = NodeSourceType.OFFLINE
            )
            assertThat(result).isTrue()
        }
}
