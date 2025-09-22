package mega.privacy.android.app.presentation.validator.toolbaractions

import android.view.MenuItem
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNode
import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNodeType
import mega.privacy.android.app.presentation.validator.toolbaractions.model.ToolbarActionsRequest
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.CloudDriveSyncsToolbarActionsModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.modifier.ToolbarActionsModifier
import mega.privacy.android.app.presentation.validator.toolbaractions.modifier.ToolbarActionsModifierItem
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.domain.entity.shares.AccessPermission
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToolbarActionsValidatorTest {

    private lateinit var underTest: ToolbarActionsValidator

    private var modifiers = setOf<ToolbarActionsModifier>()

    @BeforeEach
    fun setup() {
        modifiers = setOf()
        underTest = ToolbarActionsValidator(modifiers)
    }

    @Test
    fun `test that manageLink and removeLink are correctly displayed when an exported node selected with owner access`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        isTakenDown = false,
                        isExported = true,
                        accessPermission = AccessPermission.OWNER
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.manageLink().isVisible).isTrue()
            assertThat(actual.manageLink().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
            assertThat(actual.removeLink().isVisible).isTrue()
            assertThat(actual.link.isVisible).isFalse()
        }

    @Test
    fun `test that link is correctly displayed when a non-exported node selected with owner access`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        isTakenDown = false,
                        accessPermission = AccessPermission.OWNER
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.link.isVisible).isTrue()
            assertThat(actual.link.showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
            assertThat(actual.manageLink().isVisible).isFalse()
            assertThat(actual.removeLink().isVisible).isFalse()
        }

    @Test
    fun `test that no link options are displayed when a single node is selected without ownership and full access`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        isTakenDown = false
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.link.isVisible).isFalse()
            assertThat(actual.manageLink().isVisible).isFalse()
            assertThat(actual.removeLink().isVisible).isFalse()
        }

    @Test
    fun `test that rename option is displayed when a single node is selected with full access`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        accessPermission = AccessPermission.FULL
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.rename().isVisible).isTrue()
        }

    @Test
    fun `test that rename option is not displayed when a single node is selected without full access`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(newSelectedNode(id = 123L)),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.rename().isVisible).isFalse()
        }

    @Test
    fun `test that no link options are displayed when a single selected node is taken down`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        isTakenDown = true,
                        accessPermission = AccessPermission.OWNER
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.manageLink().isVisible).isFalse()
            assertThat(actual.removeLink().isVisible).isFalse()
            assertThat(actual.link.isVisible).isFalse()
        }

    @Test
    fun `test that link options are displayed when multiple selected nodes with owner access and not taken down`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        accessPermission = AccessPermission.OWNER
                    ),
                    newSelectedNode(
                        id = 321L,
                        accessPermission = AccessPermission.OWNER
                    )
                ),
                totalNodes = 2
            )

            val actual = underTest(request = request)

            assertThat(actual.link.isVisible).isTrue()
            assertThat(actual.link.showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

    @Test
    fun `test that link options are not displayed when multiple nodes are selected and at least one of them doesn't have owner access`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        accessPermission = AccessPermission.OWNER
                    ),
                    newSelectedNode(
                        id = 321L
                    )
                ),
                totalNodes = 2
            )

            val actual = underTest(request = request)

            assertThat(actual.link.isVisible).isFalse()
        }

    @Test
    fun `test that link options are not displayed when multiple nodes are selected and at least one of them has been taken down`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        accessPermission = AccessPermission.OWNER
                    ),
                    newSelectedNode(
                        id = 321L,
                        isTakenDown = true,
                        accessPermission = AccessPermission.OWNER
                    )
                ),
                totalNodes = 2
            )

            val actual = underTest(request = request)

            assertThat(actual.link.isVisible).isFalse()
        }

    @Test
    fun `test that the dispute option is displayed when the selected node has been taken down`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        isTakenDown = true
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.disputeTakedown().isVisible).isTrue()
            assertThat(actual.disputeTakedown().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

    @Test
    fun `test that the share out option is not displayed when the selected node has been taken down`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        isTakenDown = true
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.shareOut().isVisible).isFalse()
        }

    @Test
    fun `test that the copy option is not displayed when the selected node has been taken down`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        isTakenDown = true
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.copy().isVisible).isFalse()
        }

    @Test
    fun `test that the save to device option is not displayed when the selected node has been taken down`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        isTakenDown = true
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.saveToDevice().isVisible).isFalse()
            assertThat(actual.trash().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
            assertThat(actual.move().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
            assertThat(actual.rename().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

    @Test
    fun `test that the trash, move, and rename options are always displayed when the selected node has been taken down`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        isTakenDown = true
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.trash().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
            assertThat(actual.move().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
            assertThat(actual.rename().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

    @Test
    fun `test that the showLeaveShare option is displayed when the selected node is an incoming share node`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        isIncomingShare = true
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.leaveShare().isVisible).isTrue()
            assertThat(actual.leaveShare().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

    @Test
    fun `test that the sendToChat option is displayed when the node is a file node`() {
        val request = newToolbarActionsRequest(
            selectedNodes = listOf(newSelectedNode(id = 123L)),
            totalNodes = 1
        )

        val actual = underTest(request = request)

        assertThat(actual.sendToChat().isVisible).isTrue()
        assertThat(actual.sendToChat().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
    }

    @Test
    fun `test that the sendToChat option is not displayed when the selected node is a folder node`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        type = SelectedNodeType.Folder(
                            isShared = false,
                            isPendingShare = false,
                        ),
                        accessPermission = AccessPermission.OWNER
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.sendToChat().isVisible).isFalse()
        }

    @Test
    fun `test that the shareFolder option is not displayed when the selected node is a file node`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        accessPermission = AccessPermission.OWNER
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.shareFolder().isVisible).isFalse()
        }

    @Test
    fun `test that the shareFolder options is not displayed when multiple nodes are selected and at least one of them is an outshare folder node`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L
                    ),
                    newSelectedNode(
                        id = 321L,
                        type = SelectedNodeType.Folder(
                            isShared = true,
                            isPendingShare = false
                        ),
                    )
                ),
                totalNodes = 2
            )

            val actual = underTest(request = request)

            assertThat(actual.shareFolder().isVisible).isFalse()
        }

    @Test
    fun `test that the showTrash option is not displayed when the selected node cannot be moved to the rubbish bin`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        canBeMovedToRubbishBin = false
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.trash().isVisible).isFalse()
        }

    @Test
    fun `test that showRemoveShare option is not displayed when the selected folder node is not an outshare node`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(
                        id = 123L,
                        type = SelectedNodeType.Folder(
                            isShared = false,
                            isPendingShare = false
                        ),
                        canBeMovedToRubbishBin = false
                    )
                ),
                totalNodes = 1
            )

            val actual = underTest(request = request)

            assertThat(actual.removeShare().isVisible).isFalse()
        }

    @Test
    fun `test that the move option is always displayed when multiple nodes are selected and the total always count is less than max action count`() =
        runTest {
            val request = newToolbarActionsRequest(
                selectedNodes = listOf(
                    newSelectedNode(id = 123L),
                    newSelectedNode(id = 321L)
                ),
                totalNodes = 2
            )

            val actual = underTest(request = request)

            assertThat(actual.move().isVisible).isTrue()
            assertThat(actual.move().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

    @Test
    fun `test that the default hide and unhide options are invisible and never action`() {
        val request = newToolbarActionsRequest(
            selectedNodes = listOf(newSelectedNode(id = 123L)),
            totalNodes = 1
        )

        val actual = underTest(request = request)

        assertThat(actual.hide().isVisible).isFalse()
        assertThat(actual.hide().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_NEVER)
        assertThat(actual.unhide().isVisible).isFalse()
        assertThat(actual.unhide().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_NEVER)
    }

    @Test
    fun `test that the move option is always visible`() {
        val request = newToolbarActionsRequest(
            selectedNodes = listOf(newSelectedNode(id = 123L)),
            totalNodes = 1
        )

        val actual = underTest(request = request)

        assertThat(actual.move().isVisible).isTrue()
    }

    @Test
    fun `test that the selectAll option is displayed when not all nodes are selected`() {
        val request = newToolbarActionsRequest(
            selectedNodes = listOf(
                newSelectedNode(
                    id = 123L,
                    accessPermission = AccessPermission.OWNER
                )
            ),
            totalNodes = 5
        )

        val actual = underTest(request = request)

        assertThat(actual.selectAll().isVisible).isTrue()
    }

    @Test
    fun `test that the selectAll option is not displayed when all nodes are selected`() {
        val request = newToolbarActionsRequest(
            selectedNodes = listOf(newSelectedNode(id = 123L)),
            totalNodes = 1
        )

        val actual = underTest(request = request)

        assertThat(actual.selectAll().isVisible).isFalse()
    }

    @Test
    fun `test that the correct default options state is returned`() {
        val request = newToolbarActionsRequest(
            selectedNodes = listOf(),
            totalNodes = 10
        )

        val actual = underTest(request = request)

        assertThat(actual.selectAll().isVisible).isTrue()
        assertThat(actual.clearSelection().isVisible).isTrue()
        assertThat(actual.hide().isVisible).isFalse()
        assertThat(actual.unhide().isVisible).isFalse()
        assertThat(actual.removeLink().isVisible).isFalse()
        assertThat(actual.removeShare().isVisible).isTrue()
        assertThat(actual.rename().isVisible).isFalse()
        assertThat(actual.saveToDevice().isVisible).isTrue()
        assertThat(actual.link.isVisible).isTrue()
        assertThat(actual.manageLink().isVisible).isFalse()
        assertThat(actual.shareFolder().isVisible).isTrue()
        assertThat(actual.sendToChat().isVisible).isTrue()
        assertThat(actual.shareOut().isVisible).isTrue()
        assertThat(actual.move().isVisible).isTrue()
        assertThat(actual.copy().isVisible).isTrue()
        assertThat(actual.leaveShare().isVisible).isFalse()
        assertThat(actual.trash().isVisible).isTrue()
        assertThat(actual.removeFavourites().isVisible).isFalse()
        assertThat(actual.disputeTakedown().isVisible).isFalse()
        assertThat(actual.addToAlbum().isVisible).isFalse()
        assertThat(actual.addTo().isVisible).isFalse()
    }

    @Test
    fun `test that the correct modified control is returned`() {
        val request = newToolbarActionsRequest(
            selectedNodes = listOf(
                newSelectedNode(
                    id = 123L,
                    isTakenDown = true,
                    isIncomingShare = true,
                    type = SelectedNodeType.Folder(
                        isShared = false,
                        isPendingShare = false
                    ),
                    canBeMovedToRubbishBin = false
                )
            ),
            totalNodes = 1
        )
        val usedModifier = object : ToolbarActionsModifier {
            override fun canHandle(item: ToolbarActionsModifierItem): Boolean =
                true

            override fun modify(
                control: CloudStorageOptionControlUtil.Control,
                item: ToolbarActionsModifierItem,
            ) {
                control.selectAll().isVisible = true
                control.clearSelection().isVisible = true
                control.hide().isVisible = false
                control.unhide().isVisible = false
                control.removeLink().isVisible = false
                control.removeShare().isVisible = false
                control.rename().isVisible = false
                control.saveToDevice().isVisible = true
                control.saveToDevice().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                control.link.isVisible = false
                control.manageLink().isVisible = false
                control.shareFolder().isVisible = false
                control.sendToChat().isVisible = false
                control.shareOut().isVisible = false
                control.move().isVisible = false
                control.copy().isVisible = false
                control.leaveShare().isVisible = false
                control.trash().isVisible = true
                control.removeFavourites().isVisible = false
                control.disputeTakedown().isVisible = false
                control.addToAlbum().isVisible = false
                control.addTo().isVisible = false
            }
        }
        val allTrueModifier = object : ToolbarActionsModifier {
            override fun canHandle(item: ToolbarActionsModifierItem): Boolean =
                false

            override fun modify(
                control: CloudStorageOptionControlUtil.Control,
                item: ToolbarActionsModifierItem,
            ) {
                control.selectAll().isVisible = true
                control.clearSelection().isVisible = true
                control.hide().isVisible = true
                control.unhide().isVisible = true
                control.removeLink().isVisible = true
                control.removeShare().isVisible = true
                control.rename().isVisible = true
                control.saveToDevice().isVisible = true
                control.saveToDevice().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                control.link.isVisible = true
                control.manageLink().isVisible = true
                control.shareFolder().isVisible = true
                control.sendToChat().isVisible = true
                control.shareOut().isVisible = true
                control.move().isVisible = true
                control.copy().isVisible = true
                control.leaveShare().isVisible = true
                control.trash().isVisible = true
                control.removeFavourites().isVisible = true
                control.disputeTakedown().isVisible = true
                control.addToAlbum().isVisible = true
                control.addTo().isVisible = true
            }
        }
        modifiers = setOf(usedModifier, allTrueModifier)

        underTest = ToolbarActionsValidator(modifiers)
        val actual = underTest(request = request)

        assertThat(actual.selectAll().isVisible).isTrue()
        assertThat(actual.clearSelection().isVisible).isTrue()
        assertThat(actual.hide().isVisible).isFalse()
        assertThat(actual.unhide().isVisible).isFalse()
        assertThat(actual.removeLink().isVisible).isFalse()
        assertThat(actual.removeShare().isVisible).isFalse()
        assertThat(actual.rename().isVisible).isFalse()
        assertThat(actual.saveToDevice().isVisible).isTrue()
        assertThat(actual.saveToDevice().showAsAction).isEqualTo(MenuItem.SHOW_AS_ACTION_ALWAYS)
        assertThat(actual.link.isVisible).isFalse()
        assertThat(actual.manageLink().isVisible).isFalse()
        assertThat(actual.shareFolder().isVisible).isFalse()
        assertThat(actual.sendToChat().isVisible).isFalse()
        assertThat(actual.shareOut().isVisible).isFalse()
        assertThat(actual.move().isVisible).isFalse()
        assertThat(actual.copy().isVisible).isFalse()
        assertThat(actual.leaveShare().isVisible).isFalse()
        assertThat(actual.trash().isVisible).isTrue()
        assertThat(actual.removeFavourites().isVisible).isFalse()
        assertThat(actual.disputeTakedown().isVisible).isFalse()
        assertThat(actual.addToAlbum().isVisible).isFalse()
        assertThat(actual.addTo().isVisible).isFalse()
    }

    private fun newToolbarActionsRequest(
        modifierItem: ToolbarActionsModifierItem = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem()
        ),
        selectedNodes: List<SelectedNode> = emptyList(),
        totalNodes: Int = 0,
    ) = ToolbarActionsRequest(
        modifierItem = modifierItem,
        selectedNodes = selectedNodes,
        totalNodes = totalNodes,
    )

    private fun newSelectedNode(
        id: Long = 0L,
        type: SelectedNodeType = SelectedNodeType.File,
        isTakenDown: Boolean = false,
        isExported: Boolean = false,
        isIncomingShare: Boolean = false,
        accessPermission: AccessPermission = AccessPermission.UNKNOWN,
        canBeMovedToRubbishBin: Boolean = false,
    ) = SelectedNode(
        id = id,
        type = type,
        isTakenDown = isTakenDown,
        isExported = isExported,
        isIncomingShare = isIncomingShare,
        accessPermission = accessPermission,
        canBeMovedToRubbishBin = canBeMovedToRubbishBin,
    )
}
