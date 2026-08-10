package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveLinkMenuAction
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveLinkSelectionMenuItemTest {

    private val underTest = RemoveLinkSelectionMenuItem(mock<RemoveLinkMenuAction>())

    private fun mockFileNode(
        publicLink: String? = "https://mega.nz/link",
        isNodeKeyDecrypted: Boolean = true,
    ) = mock<TypedFileNode> {
        on { exportedData }.thenReturn(ExportedData(publicLink, 0L))
        on { this.isNodeKeyDecrypted }.thenReturn(isNodeKeyDecrypted)
    }

    @Test
    fun `test that shouldDisplay returns true when all conditions are met for single node`() =
        runTest {
            val result = underTest.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockFileNode()),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )

            assertThat(result).isTrue()
        }

    @Test
    fun `test that shouldDisplay returns true when all conditions are met for multiple nodes`() =
        runTest {
            val result = underTest.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockFileNode(), mockFileNode()),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )

            assertThat(result).isTrue()
        }

    @Test
    fun `test that shouldDisplay returns false when noNodeTakenDown is false`() =
        runTest {
            val result = underTest.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockFileNode()),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = false,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test that shouldDisplay returns false when hasNodeAccessPermission is false`() =
        runTest {
            val result = underTest.shouldDisplay(
                hasNodeAccessPermission = false,
                selectedNodes = listOf(mockFileNode()),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test that shouldDisplay returns false when publicLink is null`() =
        runTest {
            val result = underTest.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockFileNode(publicLink = null)),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test that shouldDisplay returns false when exportedData is null`() =
        runTest {
            val mockNode = mock<TypedFileNode> {
                on { isNodeKeyDecrypted }.thenReturn(true)
            }
            val result = underTest.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockNode),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test that shouldDisplay returns false when node is S4 container`() =
        runTest {
            val mockNode = mock<TypedFolderNode> {
                on { exportedData }.thenReturn(ExportedData("https://mega.nz/link", 0L))
                on { isNodeKeyDecrypted }.thenReturn(true)
                on { isS4Container }.thenReturn(true)
            }
            val result = underTest.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockNode),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test that shouldDisplay returns false when isNodeKeyDecrypted is false`() =
        runTest {
            val result = underTest.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(mockFileNode(isNodeKeyDecrypted = false)),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )

            assertThat(result).isFalse()
        }

    @Test
    fun `test that shouldDisplay returns false when any node in multi-selection fails checks`() =
        runTest {
            val result = underTest.shouldDisplay(
                hasNodeAccessPermission = true,
                selectedNodes = listOf(
                    mockFileNode(),
                    mockFileNode(publicLink = null)
                ),
                canBeMovedToTarget = false,
                noNodeInBackups = false,
                noNodeTakenDown = true,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )

            assertThat(result).isFalse()
        }
}
