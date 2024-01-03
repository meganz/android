package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.SlideshowMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SlideshowBottomSheetMenuItemTest {
    private val underTest = SlideshowBottomSheetMenuItem(menuAction = SlideshowMenuAction())

    @Test
    fun `test that shouldDisplay returns false when node is not taken down and not in rubbish`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { isTakenDown } doReturn false
            }
            val result = underTest.shouldDisplay(
                isNodeInRubbish = false,
                accessPermission = null,
                isInBackups = false,
                node = node,
                true
            )
            Truth.assertThat(result).isFalse()
        }

    @Test
    fun `test that shouldDisplay returns false when node is taken down`() = runTest {
        val node = mock<TypedFileNode> {
            on { isTakenDown } doReturn true
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            true
        )
        Truth.assertThat(result).isFalse()
    }
}