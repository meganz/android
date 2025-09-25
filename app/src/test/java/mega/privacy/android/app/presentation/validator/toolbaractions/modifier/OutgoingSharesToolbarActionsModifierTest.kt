package mega.privacy.android.app.presentation.validator.toolbaractions.modifier

import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.CloudDriveSyncsToolbarActionsModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.OutgoingSharesAddToActionModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.OutgoingSharesToolbarActionsModifierItem
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OutgoingSharesToolbarActionsModifierTest {

    private lateinit var underTest: OutgoingSharesToolbarActionsModifier

    @BeforeEach
    fun setup() {
        underTest = OutgoingSharesToolbarActionsModifier()
    }

    @Test
    fun `test that true is returned when the modifier item is for outgoing shares`() {
        val item = ToolbarActionsModifierItem.OutgoingShares(
            item = OutgoingSharesToolbarActionsModifierItem()
        )

        val actual = underTest.canHandle(item = item)

        assertThat(actual).isTrue()
    }

    @Test
    fun `test that false is returned when the modifier item is not for outgoing shares`() {
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem()
        )

        val actual = underTest.canHandle(item = item)

        assertThat(actual).isFalse()
    }

    @Test
    fun `test that the move option is not displayed`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.OutgoingShares(
            item = OutgoingSharesToolbarActionsModifierItem()
        )

        underTest.modify(
            control = control,
            item = item
        )

        assertThat(control.move().isVisible).isFalse()
    }

    @Test
    fun `test that hide and unhide options are not displayed`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.OutgoingShares(
            item = OutgoingSharesToolbarActionsModifierItem()
        )

        underTest.modify(
            control = control,
            item = item
        )

        assertThat(control.hide().isVisible).isFalse()
        assertThat(control.unhide().isVisible).isFalse()
    }

    @Test
    fun `test that remove share option is always displayed when all the nodes are not taken down and is in root level`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.OutgoingShares(
            item = OutgoingSharesToolbarActionsModifierItem(
                areAllNotTakenDown = true,
                isRootLevel = true
            )
        )

        underTest.modify(
            control = control,
            item = item
        )

        assertThat(control.removeShare().isVisible).isTrue()
        assertThat(control.removeShare().showAsAction).isEqualTo(SHOW_AS_ACTION_ALWAYS)
    }

    @Test
    fun `test that share out option is always displayed when all the nodes are not taken down`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.OutgoingShares(
            item = OutgoingSharesToolbarActionsModifierItem(
                areAllNotTakenDown = true
            )
        )

        underTest.modify(
            control = control,
            item = item
        )

        assertThat(control.shareOut().isVisible).isTrue()
        assertThat(control.shareOut().showAsAction).isEqualTo(SHOW_AS_ACTION_ALWAYS)
    }

    @Test
    fun `test that all link options are not displayed when all the nodes are not taken down and should hide link`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.OutgoingShares(
            item = OutgoingSharesToolbarActionsModifierItem(
                areAllNotTakenDown = true,
                shouldHideLink = true
            )
        )

        underTest.modify(
            control = control,
            item = item
        )

        assertThat(control.manageLink().isVisible).isFalse()
        assertThat(control.removeLink().isVisible).isFalse()
        assertThat(control.link.isVisible).isFalse()
    }

    @Test
    fun `test that copy option is always displayed when all the nodes are not taken down and the total number of always displayed options is less than the maximum count`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.OutgoingShares(
            item = OutgoingSharesToolbarActionsModifierItem(
                areAllNotTakenDown = true
            )
        )

        underTest.modify(
            control = control,
            item = item
        )

        assertThat(control.copy().isVisible).isTrue()
        assertThat(control.copy().showAsAction).isEqualTo(SHOW_AS_ACTION_ALWAYS)
    }

    @Test
    fun `test that copy option is not always displayed when all the nodes are not taken down and the total number of always displayed options is more than the maximum count`() {
        val control = CloudStorageOptionControlUtil.Control()
        control.selectAll().isVisible = true
        control.selectAll().showAsAction = SHOW_AS_ACTION_ALWAYS
        control.addLabel().isVisible = true
        control.addLabel().showAsAction = SHOW_AS_ACTION_ALWAYS
        control.addFavourites().isVisible = true
        control.addFavourites().showAsAction = SHOW_AS_ACTION_ALWAYS
        control.clearSelection().isVisible = true
        control.clearSelection().showAsAction = SHOW_AS_ACTION_ALWAYS
        val item = ToolbarActionsModifierItem.OutgoingShares(
            item = OutgoingSharesToolbarActionsModifierItem(
                areAllNotTakenDown = true
            )
        )

        underTest.modify(
            control = control,
            item = item
        )

        assertThat(control.copy().isVisible).isTrue()
        assertThat(control.copy().showAsAction).isEqualTo(SHOW_AS_ACTION_NEVER)
    }

    @Test
    fun `test that the addToAlbum option is displayed when the node can be added to an album`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.OutgoingShares(
            item = OutgoingSharesToolbarActionsModifierItem(
                addToItem = OutgoingSharesAddToActionModifierItem(canBeAddedToAlbum = true)
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.addToAlbum().isVisible).isTrue()
    }

    @Test
    fun `test that the addToAlbum option is not displayed when the node cannot be added to an album`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.OutgoingShares(
            item = OutgoingSharesToolbarActionsModifierItem(
                addToItem = OutgoingSharesAddToActionModifierItem(canBeAddedToAlbum = false)
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.addToAlbum().isVisible).isFalse()
    }

    @Test
    fun `test that the addTo option is displayed when the node can be added to an album`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.OutgoingShares(
            item = OutgoingSharesToolbarActionsModifierItem(
                addToItem = OutgoingSharesAddToActionModifierItem(canBeAddedTo = true)
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.addTo().isVisible).isTrue()
    }

    @Test
    fun `test that the addTo option is not displayed when the node cannot be added to an album`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.OutgoingShares(
            item = OutgoingSharesToolbarActionsModifierItem(
                addToItem = OutgoingSharesAddToActionModifierItem(canBeAddedTo = false)
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.addTo().isVisible).isFalse()
    }
}
