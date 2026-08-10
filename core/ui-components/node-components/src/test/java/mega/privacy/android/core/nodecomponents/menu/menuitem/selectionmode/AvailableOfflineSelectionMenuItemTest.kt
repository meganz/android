package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AvailableOfflineSelectionMenuItemTest {

    private val mockOfflineNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(123L)
        on { isAvailableOffline } doReturn true
    }

    private val mockNonOfflineNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(456L)
        on { isAvailableOffline } doReturn false
    }

    private val mockAnotherNonOfflineNode = mock<TypedFileNode> {
        on { id } doReturn NodeId(789L)
        on { isAvailableOffline } doReturn false
    }

    private val underTest = AvailableOfflineSelectionMenuItem(
        mock<AvailableOfflineMenuAction>(),
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
            "single_non_offline" -> listOf(mockNonOfflineNode)
            "multiple_non_offline" -> listOf(mockNonOfflineNode, mockAnotherNonOfflineNode)
            "all_offline" -> listOf(mockOfflineNode)
            "mixed" -> listOf(mockNonOfflineNode, mockOfflineNode)
            else -> emptyList()
        }

        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = selectedNodes,
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = noNodeTakenDown,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test shouldDisplay returns false when any node is S4 container`() = runTest {
        val s4ContainerNode = mock<TypedFolderNode> {
            on { id } doReturn NodeId(321L)
            on { isAvailableOffline } doReturn false
            on { isS4Container } doReturn true
        }

        val result = underTest.shouldDisplay(
            hasNodeAccessPermission = true,
            selectedNodes = listOf(s4ContainerNode),
            canBeMovedToTarget = true,
            noNodeInBackups = true,
            noNodeTakenDown = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )

        assertThat(result).isFalse()
    }

    companion object {
        @JvmStatic
        fun provideShouldDisplayParameters(): Stream<Arguments> = Stream.of(
            Arguments.of(true, "empty", false),
            Arguments.of(false, "single_non_offline", false),
            Arguments.of(true, "all_offline", false),
            Arguments.of(true, "mixed", false),
            Arguments.of(true, "single_non_offline", true),
            Arguments.of(true, "multiple_non_offline", true),
        )
    }
}
