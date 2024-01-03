package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.model.menuaction.DisputeTakeDownMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DisputeTakeDownBottomSheetMenuItemTest {
    private val underTest =
        DisputeTakeDownBottomSheetMenuItem(menuAction = DisputeTakeDownMenuAction())

    @Test
    fun `test that shouldDisplay returns true when node is taken down`() = runTest {
        val node = mock<TypedFileNode> {
            on { isTakenDown } doReturn true
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )
        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `test that shouldDisplay returns false when node is not taken down`() = runTest {
        val node = mock<TypedFileNode> {
            on { isTakenDown } doReturn false
        }
        val result = underTest.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = node,
            isConnected = true
        )
        Truth.assertThat(result).isFalse()
    }

}