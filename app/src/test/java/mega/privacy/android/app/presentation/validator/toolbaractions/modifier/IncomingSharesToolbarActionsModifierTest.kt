package mega.privacy.android.app.presentation.validator.toolbaractions.modifier

import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.CloudDriveSyncsToolbarActionsModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.IncomingSharesCopyActionModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.IncomingSharesMoveActionModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.IncomingSharesRenameActionModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.IncomingSharesToolbarActionsModifierItem
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IncomingSharesToolbarActionsModifierTest {

    private lateinit var underTest: IncomingSharesToolbarActionsModifier

    @BeforeEach
    fun setup() {
        underTest = IncomingSharesToolbarActionsModifier()
    }

    @Test
    fun `test that true is returned when the modifier item is for incoming shares`() {
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem()
        )

        val actual = underTest.canHandle(item = item)

        assertThat(actual).isTrue()
    }

    @Test
    fun `test that false is returned when the modifier item is not for incoming shares`() {
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem()
        )

        val actual = underTest.canHandle(item = item)

        assertThat(actual).isFalse()
    }

    @Test
    fun `test that hide and unhide options are not displayed`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem()
        )

        underTest.modify(control = control, item = item)

        assertThat(control.hide().isVisible).isFalse()
        assertThat(control.unhide().isVisible).isFalse()
    }

    @Test
    fun `test that share folder option is not displayed`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem()
        )

        underTest.modify(control = control, item = item)

        assertThat(control.shareFolder().isVisible).isFalse()
    }

    @Test
    fun `test that share out option is not displayed`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem()
        )

        underTest.modify(control = control, item = item)

        assertThat(control.shareOut().isVisible).isFalse()
    }

    @Test
    fun `test that rename option is not displayed when disabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem(
                renameItem = IncomingSharesRenameActionModifierItem(
                    isEnabled = false
                )
            )
        )

        underTest.modify(control = control, item = item)

        assertThat(control.rename().isVisible).isFalse()
    }

    @Test
    fun `test that rename option is displayed when enabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem(
                renameItem = IncomingSharesRenameActionModifierItem(
                    isEnabled = true
                )
            )
        )

        underTest.modify(control = control, item = item)

        assertThat(control.rename().isVisible).isTrue()
    }

    @Test
    fun `test that rename option is always displayed when it is enabled and the total number of always displayed options is less than the maximum count`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem(
                renameItem = IncomingSharesRenameActionModifierItem(
                    isEnabled = true
                )
            )
        )

        underTest.modify(control = control, item = item)

        assertThat(control.rename().showAsAction).isEqualTo(SHOW_AS_ACTION_ALWAYS)
    }

    @Test
    fun `test that rename option is not always displayed when it is enabled and the total number of always displayed options is more than the maximum count`() {
        val control = CloudStorageOptionControlUtil.Control()
        control.selectAll().isVisible = true
        control.selectAll().showAsAction = SHOW_AS_ACTION_ALWAYS
        control.addLabel().isVisible = true
        control.addLabel().showAsAction = SHOW_AS_ACTION_ALWAYS
        control.addFavourites().isVisible = true
        control.addFavourites().showAsAction = SHOW_AS_ACTION_ALWAYS
        control.clearSelection().isVisible = true
        control.clearSelection().showAsAction = SHOW_AS_ACTION_ALWAYS
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem(
                renameItem = IncomingSharesRenameActionModifierItem(
                    isEnabled = true
                )
            )
        )

        underTest.modify(control = control, item = item)

        assertThat(control.rename().showAsAction).isEqualTo(SHOW_AS_ACTION_NEVER)
    }

    @Test
    fun `test that the move option is not displayed when disabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem(
                moveItem = IncomingSharesMoveActionModifierItem(
                    isEnabled = false
                )
            )
        )

        underTest.modify(control = control, item = item)

        assertThat(control.move().isVisible).isFalse()
    }

    @Test
    fun `test that the move option is displayed when enabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem(
                moveItem = IncomingSharesMoveActionModifierItem(
                    isEnabled = true
                )
            )
        )

        underTest.modify(control = control, item = item)

        assertThat(control.move().isVisible).isTrue()
    }

    @Test
    fun `test that the move option is always displayed when it is enabled and the total number of always displayed options is less than the maximum count`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem(
                moveItem = IncomingSharesMoveActionModifierItem(
                    isEnabled = true
                )
            )
        )

        underTest.modify(control = control, item = item)

        assertThat(control.move().showAsAction).isEqualTo(SHOW_AS_ACTION_ALWAYS)
    }

    @Test
    fun `test that move option is not always displayed when it is enabled and the total number of always displayed options is more than the maximum count`() {
        val control = CloudStorageOptionControlUtil.Control()
        control.selectAll().isVisible = true
        control.selectAll().showAsAction = SHOW_AS_ACTION_ALWAYS
        control.addLabel().isVisible = true
        control.addLabel().showAsAction = SHOW_AS_ACTION_ALWAYS
        control.addFavourites().isVisible = true
        control.addFavourites().showAsAction = SHOW_AS_ACTION_ALWAYS
        control.clearSelection().isVisible = true
        control.clearSelection().showAsAction = SHOW_AS_ACTION_ALWAYS
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem(
                moveItem = IncomingSharesMoveActionModifierItem(
                    isEnabled = true
                )
            )
        )

        underTest.modify(control = control, item = item)

        assertThat(control.move().showAsAction).isEqualTo(SHOW_AS_ACTION_NEVER)
    }

    @Test
    fun `test that the copy option is always displayed when enabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem(
                copyItem = IncomingSharesCopyActionModifierItem(
                    isEnabled = true
                )
            )
        )

        underTest.modify(control = control, item = item)

        assertThat(control.copy().isVisible).isTrue()
        assertThat(control.copy().showAsAction).isEqualTo(SHOW_AS_ACTION_ALWAYS)
    }

    @Test
    fun `test that the save to device option is not displayed when the copy item is disabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem(
                copyItem = IncomingSharesCopyActionModifierItem(
                    isEnabled = false
                )
            )
        )

        underTest.modify(control = control, item = item)

        assertThat(control.saveToDevice().isVisible).isFalse()
    }

    @Test
    fun `test that the trash option is displayed when the move item is enabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem(
                moveItem = IncomingSharesMoveActionModifierItem(
                    isEnabled = true
                )
            )
        )

        underTest.modify(control = control, item = item)

        assertThat(control.trash().isVisible).isTrue()
    }

    @Test
    fun `test that the trash option is not displayed when the move item is disabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem(
                moveItem = IncomingSharesMoveActionModifierItem(
                    isEnabled = false
                )
            )
        )

        underTest.modify(control = control, item = item)

        assertThat(control.trash().isVisible).isFalse()
    }

    @Test
    fun `test that the add to album option is not displayed`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem()
        )

        underTest.modify(control = control, item = item)

        assertThat(control.addToAlbum().isVisible).isFalse()
    }

    @Test
    fun `test that the add to option is not displayed`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.IncomingShares(
            item = IncomingSharesToolbarActionsModifierItem()
        )

        underTest.modify(control = control, item = item)

        assertThat(control.addTo().isVisible).isFalse()
    }
}
