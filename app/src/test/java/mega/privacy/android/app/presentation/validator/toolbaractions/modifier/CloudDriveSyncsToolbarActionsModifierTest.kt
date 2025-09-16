package mega.privacy.android.app.presentation.validator.toolbaractions.modifier

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.CloudDriveSyncsAddLabelActionModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.CloudDriveSyncsAddToActionModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.CloudDriveSyncsFavouritesActionModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.CloudDriveSyncsHiddenNodeActionModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.CloudDriveSyncsToolbarActionsModifierItem
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudDriveSyncsToolbarActionsModifierTest {

    private lateinit var underTest: CloudDriveSyncsToolbarActionsModifier

    @BeforeEach
    fun setup() {
        underTest = CloudDriveSyncsToolbarActionsModifier()
    }

    @Test
    fun `test that true is returned when the modifier item is for cloud drive syncs`() {
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem()
        )

        val actual = underTest.canHandle(item = item)

        assertThat(actual).isTrue()
    }

    @Test
    fun `test that the hide and unhide options are not displayed when the hidden node item is disabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        control.hide().isVisible = true
        control.unhide().isVisible = true
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                hiddenNodeItem = CloudDriveSyncsHiddenNodeActionModifierItem(isEnabled = false)
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.hide().isVisible).isFalse()
        assertThat(control.unhide().isVisible).isFalse()
    }

    @Test
    fun `test that the hide option is displayed when the node can be hidden and the hidden node item is enabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                hiddenNodeItem = CloudDriveSyncsHiddenNodeActionModifierItem(
                    isEnabled = true,
                    canBeHidden = true
                )
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.hide().isVisible).isTrue()
    }

    @Test
    fun `test that the unhide option is not displayed when the node can be hidden and the hidden node item is enabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                hiddenNodeItem = CloudDriveSyncsHiddenNodeActionModifierItem(
                    isEnabled = true,
                    canBeHidden = true
                )
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.unhide().isVisible).isFalse()
    }

    @Test
    fun `test that the hide option is not displayed when the node cannot be hidden and the hidden node item is enabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                hiddenNodeItem = CloudDriveSyncsHiddenNodeActionModifierItem(
                    isEnabled = true,
                    canBeHidden = false
                )
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.hide().isVisible).isFalse()
    }

    @Test
    fun `test that the unhide option is displayed when the node cannot be hidden and the hidden node item is enabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                hiddenNodeItem = CloudDriveSyncsHiddenNodeActionModifierItem(
                    isEnabled = true,
                    canBeHidden = false
                )
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.unhide().isVisible).isTrue()
    }

    @Test
    fun `test that the addToAlbum option is displayed when the node can be added to an album`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                addToItem = CloudDriveSyncsAddToActionModifierItem(canBeAddedToAlbum = true)
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
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                addToItem = CloudDriveSyncsAddToActionModifierItem(canBeAddedToAlbum = false)
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
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                addToItem = CloudDriveSyncsAddToActionModifierItem(canBeAddedTo = true)
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
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                addToItem = CloudDriveSyncsAddToActionModifierItem(canBeAddedTo = false)
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.addTo().isVisible).isFalse()
    }

    @Test
    fun `test that the addFavourites option is displayed when the node can be added`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                favouritesItem = CloudDriveSyncsFavouritesActionModifierItem(canBeAdded = true)
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.addFavourites().isVisible).isTrue()
    }

    @Test
    fun `test that the addFavourites option is not displayed when the node cannot be added`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                favouritesItem = CloudDriveSyncsFavouritesActionModifierItem(canBeAdded = false)
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.addFavourites().isVisible).isFalse()
    }

    @Test
    fun `test that the removeFavourites option is displayed when the node can be removed from favourites`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                favouritesItem = CloudDriveSyncsFavouritesActionModifierItem(canBeRemoved = true)
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.removeFavourites().isVisible).isTrue()
    }

    @Test
    fun `test that the removeFavourites option is not displayed when the node cannot be removed from favourites`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                favouritesItem = CloudDriveSyncsFavouritesActionModifierItem(canBeRemoved = false)
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.removeFavourites().isVisible).isFalse()
    }

    @Test
    fun `test that the addLabel option is displayed when a label can be added to the node`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                addLabelItem = CloudDriveSyncsAddLabelActionModifierItem(canBeAdded = true)
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.addLabel().isVisible).isTrue()
    }

    @Test
    fun `test that the addLabel option is not displayed when a label cannot be added to the node`() {
        val control = CloudStorageOptionControlUtil.Control()
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem(
                addLabelItem = CloudDriveSyncsAddLabelActionModifierItem(canBeAdded = false)
            )
        )

        underTest.modify(
            control = control,
            item = item,
        )

        assertThat(control.addLabel().isVisible).isFalse()
    }
}
