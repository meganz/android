package mega.privacy.mobile.home.presentation.recents.bucket

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.model.NodeUiItem
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecentsBucketTakenDownClickTest {

    private fun item(
        isTakenDown: Boolean,
        isFolderNode: Boolean = false,
    ): NodeUiItem<TypedNode> = NodeUiItem(
        node = mock<TypedNode> { on { it.isTakenDown } doReturn isTakenDown },
        isSelected = false,
        isFolderNode = isFolderNode,
    )

    @Test
    fun `test that shouldDisputeTakenDownOnClick returns true for a taken down file outside selection mode`() {
        val result = item(isTakenDown = true).shouldDisputeTakenDownOnClick(isInSelectionMode = false)

        assertThat(result).isTrue()
    }

    @Test
    fun `test that shouldDisputeTakenDownOnClick returns false in selection mode`() {
        val result = item(isTakenDown = true).shouldDisputeTakenDownOnClick(isInSelectionMode = true)

        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisputeTakenDownOnClick returns false for a node that is not taken down`() {
        val result = item(isTakenDown = false).shouldDisputeTakenDownOnClick(isInSelectionMode = false)

        assertThat(result).isFalse()
    }

    @Test
    fun `test that shouldDisputeTakenDownOnClick returns false for a taken down folder`() {
        val result = item(isTakenDown = true, isFolderNode = true)
            .shouldDisputeTakenDownOnClick(isInSelectionMode = false)

        assertThat(result).isFalse()
    }
}
