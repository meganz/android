package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.FavouriteMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddToFavouritesSelectionMenuItemTest {

    private val mockFavouriteNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(123L)
        on { isFavourite } doReturn true
    }

    private val mockNonFavouriteNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(456L)
        on { isFavourite } doReturn false
    }

    @ParameterizedTest(name = "noNodeTakenDown={0}, selectedNodesKind={1} -> expected={2}")
    @MethodSource("provideShouldDisplayParameters")
    fun `test shouldDisplay returns expected result`(
        noNodeTakenDown: Boolean,
        selectedNodesKind: String,
        expected: Boolean,
    ) = runTest {
        val selectedNodes = when (selectedNodesKind) {
            "empty" -> emptyList()
            "all_favourite" -> listOf(mockFavouriteNode)
            "single_non_favourite" -> listOf(mockNonFavouriteNode)
            "mixed" -> listOf(mockFavouriteNode, mockNonFavouriteNode)
            else -> emptyList()
        }
        val menuItem = AddToFavouritesSelectionMenuItem(
            mock<FavouriteMenuAction>(),
        )

        val result = menuItem.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = selectedNodes,
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = noNodeTakenDown,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        fun provideShouldDisplayParameters(): Stream<Arguments> = Stream.of(
            Arguments.of(true, "empty", false),
            Arguments.of(false, "single_non_favourite", false),
            Arguments.of(true, "all_favourite", false),
            Arguments.of(true, "mixed", true),
            Arguments.of(true, "single_non_favourite", true),
        )
    }
}
