package mega.privacy.android.app.presentation.imagepreview.menu

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudDriveImagePreviewMenuTest {

    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()

    private val nodeId = NodeId(1L)
    private val imageNode = mock<ImageNode> { on { id } doReturn nodeId }

    private lateinit var underTest: CloudDriveImagePreviewMenu

    @BeforeEach
    fun setUp() {
        reset(getNodeAccessPermission, isNodeInBackupsUseCase)
        underTest = CloudDriveImagePreviewMenu(
            getNodeAccessPermission = getNodeAccessPermission,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
        )
    }

    private suspend fun stubInBackups(inBackups: Boolean) {
        whenever(isNodeInBackupsUseCase(nodeId.longValue)).thenReturn(inBackups)
    }

    @Test
    fun `test that isRenameMenuVisible returns false when node is in backups`() = runTest {
        stubInBackups(true)
        assertThat(underTest.isRenameMenuVisible(imageNode)).isFalse()
    }

    @Test
    fun `test that isRenameMenuVisible returns true when node is not in backups`() = runTest {
        stubInBackups(false)
        assertThat(underTest.isRenameMenuVisible(imageNode)).isTrue()
    }

    @Test
    fun `test that isMoveMenuVisible returns false when node is in backups`() = runTest {
        stubInBackups(true)
        assertThat(underTest.isMoveMenuVisible(imageNode)).isFalse()
    }

    @Test
    fun `test that isMoveMenuVisible returns true when node is not in backups`() = runTest {
        stubInBackups(false)
        assertThat(underTest.isMoveMenuVisible(imageNode)).isTrue()
    }

    @Test
    fun `test that isMoveToRubbishBinMenuVisible returns false when node is in backups`() =
        runTest {
            stubInBackups(true)
            assertThat(underTest.isMoveToRubbishBinMenuVisible(imageNode)).isFalse()
        }

    @Test
    fun `test that isMoveToRubbishBinMenuVisible returns true when node is not in backups`() =
        runTest {
            stubInBackups(false)
            assertThat(underTest.isMoveToRubbishBinMenuVisible(imageNode)).isTrue()
        }

    @Test
    fun `test that isFavouriteMenuVisible returns false when node is in backups`() = runTest {
        stubInBackups(true)
        assertThat(underTest.isFavouriteMenuVisible(imageNode)).isFalse()
    }

    @Test
    fun `test that isFavouriteMenuVisible returns true when node is not in backups`() = runTest {
        stubInBackups(false)
        assertThat(underTest.isFavouriteMenuVisible(imageNode)).isTrue()
    }

    @Test
    fun `test that isLabelMenuVisible returns false when node is in backups`() = runTest {
        stubInBackups(true)
        assertThat(underTest.isLabelMenuVisible(imageNode)).isFalse()
    }

    @Test
    fun `test that isLabelMenuVisible returns true when node is not in backups`() = runTest {
        stubInBackups(false)
        assertThat(underTest.isLabelMenuVisible(imageNode)).isTrue()
    }

    @Test
    fun `test that isHideMenuVisible returns false when node is in backups even with owner access`() =
        runTest {
            whenever(imageNode.isMarkedSensitive).thenReturn(false)
            whenever(imageNode.isSensitiveInherited).thenReturn(false)
            whenever(getNodeAccessPermission(nodeId)).thenReturn(AccessPermission.OWNER)
            stubInBackups(true)
            assertThat(underTest.isHideMenuVisible(imageNode)).isFalse()
        }

    @Test
    fun `test that isHideMenuVisible returns true when node is not in backups with owner access`() =
        runTest {
            whenever(imageNode.isMarkedSensitive).thenReturn(false)
            whenever(imageNode.isSensitiveInherited).thenReturn(false)
            whenever(getNodeAccessPermission(nodeId)).thenReturn(AccessPermission.OWNER)
            stubInBackups(false)
            assertThat(underTest.isHideMenuVisible(imageNode)).isTrue()
        }

    @Test
    fun `test that isUnhideMenuVisible returns false when node is in backups even with owner access`() =
        runTest {
            whenever(imageNode.isMarkedSensitive).thenReturn(true)
            whenever(imageNode.isSensitiveInherited).thenReturn(false)
            whenever(getNodeAccessPermission(nodeId)).thenReturn(AccessPermission.OWNER)
            stubInBackups(true)
            assertThat(underTest.isUnhideMenuVisible(imageNode)).isFalse()
        }

    @Test
    fun `test that isUnhideMenuVisible returns true when node is not in backups with owner access`() =
        runTest {
            whenever(imageNode.isMarkedSensitive).thenReturn(true)
            whenever(imageNode.isSensitiveInherited).thenReturn(false)
            whenever(getNodeAccessPermission(nodeId)).thenReturn(AccessPermission.OWNER)
            stubInBackups(false)
            assertThat(underTest.isUnhideMenuVisible(imageNode)).isTrue()
        }

    @Test
    fun `test that isNodeInBackupsUseCase is invoked only once for multiple menu queries on the same node`() =
        runTest {
            stubInBackups(true)

            underTest.isRenameMenuVisible(imageNode)
            underTest.isMoveMenuVisible(imageNode)
            underTest.isFavouriteMenuVisible(imageNode)
            underTest.isLabelMenuVisible(imageNode)
            underTest.isMoveToRubbishBinMenuVisible(imageNode)

            verify(isNodeInBackupsUseCase, times(1)).invoke(nodeId.longValue)
        }

    @Test
    fun `test that isNodeInBackupsUseCase is invoked again when a different node is queried`() =
        runTest {
            val otherNodeId = NodeId(2L)
            val otherImageNode = mock<ImageNode> { on { id } doReturn otherNodeId }
            whenever(isNodeInBackupsUseCase(nodeId.longValue)).thenReturn(true)
            whenever(isNodeInBackupsUseCase(otherNodeId.longValue)).thenReturn(false)

            assertThat(underTest.isRenameMenuVisible(imageNode)).isFalse()
            assertThat(underTest.isRenameMenuVisible(otherImageNode)).isTrue()

            verify(isNodeInBackupsUseCase, times(1)).invoke(nodeId.longValue)
            verify(isNodeInBackupsUseCase, times(1)).invoke(otherNodeId.longValue)
        }
}
