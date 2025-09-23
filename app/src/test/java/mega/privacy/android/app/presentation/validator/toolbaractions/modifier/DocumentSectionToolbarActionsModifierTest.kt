package mega.privacy.android.app.presentation.validator.toolbaractions.modifier

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.CloudDriveSyncsToolbarActionsModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.DocumentSectionHiddenNodeActionModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.DocumentSectionToolbarActionsModifierItem
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentSectionToolbarActionsModifierTest {

    private lateinit var underTest: DocumentSectionToolbarActionsModifier

    @BeforeEach
    fun setup() {
        underTest = DocumentSectionToolbarActionsModifier()
    }

    @Test
    fun `test that true is returned when the modifier item is for audio section`() {
        val item = ToolbarActionsModifierItem.DocumentSection(
            item = DocumentSectionToolbarActionsModifierItem()
        )

        val actual = underTest.canHandle(item = item)

        assertThat(actual).isTrue()
    }

    @Test
    fun `test that false is returned when the modifier item is not for audio section`() {
        val item = ToolbarActionsModifierItem.CloudDriveSyncs(
            item = CloudDriveSyncsToolbarActionsModifierItem()
        )

        val actual = underTest.canHandle(item = item)

        assertThat(actual).isFalse()
    }

    @Test
    fun `test that the hide and unhide options are not displayed when the hidden node item is disabled`() {
        val control = CloudStorageOptionControlUtil.Control()
        control.hide().isVisible = true
        control.unhide().isVisible = true
        val item = ToolbarActionsModifierItem.DocumentSection(
            item = DocumentSectionToolbarActionsModifierItem(
                hiddenNodeItem = DocumentSectionHiddenNodeActionModifierItem(isEnabled = false)
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
        val item = ToolbarActionsModifierItem.DocumentSection(
            item = DocumentSectionToolbarActionsModifierItem(
                hiddenNodeItem = DocumentSectionHiddenNodeActionModifierItem(
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
        val item = ToolbarActionsModifierItem.DocumentSection(
            item = DocumentSectionToolbarActionsModifierItem(
                hiddenNodeItem = DocumentSectionHiddenNodeActionModifierItem(
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
        val item = ToolbarActionsModifierItem.DocumentSection(
            item = DocumentSectionToolbarActionsModifierItem(
                hiddenNodeItem = DocumentSectionHiddenNodeActionModifierItem(
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
        val item = ToolbarActionsModifierItem.DocumentSection(
            item = DocumentSectionToolbarActionsModifierItem(
                hiddenNodeItem = DocumentSectionHiddenNodeActionModifierItem(
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
}
