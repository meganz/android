package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveOfflineMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveOfflineSelectionMenuItemTest {

    private val mockAvailableOfflineNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(123L)
        on { isAvailableOffline } doReturn true
    }

    private val mockNonAvailableOfflineNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(456L)
        on { isAvailableOffline } doReturn false
    }

    private val mockAnotherAvailableOfflineNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(789L)
        on { isAvailableOffline } doReturn true
    }

    private val underTest = RemoveOfflineSelectionMenuItem(
        mock<RemoveOfflineMenuAction>(),
    )

    @ParameterizedTest(name = "noNodeTakenDown={0}, selectedNodesKind={1} -> expected={2}")
    @MethodSource("provideShouldDisplayParameters")
    fun `test shouldDisplay returns expected result`(
        noNodeTakenDown: Boolean,
        selectedNodesKind: String,
        expected: Boolean,
    ) = runTest {
        val selectedNodes = when (selectedNodesKind) {
            "empty" -> emptyList()
            "single_offline" -> listOf(mockAvailableOfflineNode)
            "multiple_offline" -> listOf(mockAvailableOfflineNode, mockAnotherAvailableOfflineNode)
            "mixed" -> listOf(mockAvailableOfflineNode, mockNonAvailableOfflineNode)
            else -> emptyList()
        }

        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = selectedNodes,
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = noNodeTakenDown,
            nodeSourceType = NodeSourceType.OFFLINE
        )

        assertThat(result).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        fun provideShouldDisplayParameters(): Stream<Arguments> = Stream.of(
            Arguments.of(true, "empty", false),
            Arguments.of(false, "single_offline", false),
            Arguments.of(true, "mixed", false),
            Arguments.of(true, "single_offline", true),
            Arguments.of(true, "multiple_offline", true),
        )
    }
}
