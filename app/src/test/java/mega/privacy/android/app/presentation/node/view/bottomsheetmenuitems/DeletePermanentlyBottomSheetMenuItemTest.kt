package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.node.model.menuaction.DeletePermanentlyMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeletePermanentlyBottomSheetMenuItemTest {
    private val underTest =
        DeletePermanentlyBottomSheetMenuItem(menuAction = DeletePermanentlyMenuAction())
    private val node = mock<TypedFileNode> {
        on { isTakenDown } doReturn false
    }

    @Test
    fun `test that shouldDisplay returns true when node is in rubbish`() {
        val result = underTest.shouldDisplay(
            isNodeInRubbish = true,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )

        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `test that shouldDisplay returns false when node is not in rubbish`() {
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )

        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns true when node is taken down`() {
        val node = mock<TypedFileNode> {
            on { isTakenDown } doReturn true
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = true,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )

        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `test that shouldDisplay returns true when node is in rubbish and taken down`() {
        val node = mock<TypedFileNode> {
            on { isTakenDown } doReturn true
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = true,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )

        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `test that shouldDisplay returns false when node is in backups`() {
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = true,
            node = node,
            isConnected = true
        )

        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisplay returns true when node is in rubbish and in backups`() {
        val result = underTest.shouldDisplay(
            isNodeInRubbish = true,
            accessPermission = null,
            isInBackups = true,
            node = node,
            isConnected = true
        )

        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `test that shouldDisplay returns false when node is not in rubbish and in backups`() {
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = true,
            node = node,
            isConnected = true
        )

        Truth.assertThat(result).isFalse()
    }

}