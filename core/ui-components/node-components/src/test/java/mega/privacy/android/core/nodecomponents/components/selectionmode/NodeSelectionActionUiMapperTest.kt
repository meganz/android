package mega.privacy.android.core.nodecomponents.components.selectionmode

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionHandler
import mega.privacy.android.core.nodecomponents.model.NodeSelectionModeMenuItem
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeSelectionActionUiMapperTest {
    private lateinit var underTest: NodeSelectionActionUiMapper

    @BeforeAll
    fun setup() {
        underTest = NodeSelectionActionUiMapper()
    }

    @ParameterizedTest
    @MethodSource("provideMenuActionMappings")
    fun `test that mapper correctly maps menu actions to NodeSelectionAction`(
        menuAction: MenuActionWithIcon,
        expectedAction: NodeSelectionAction,
    ) {
        val mockHandler = mock<NodeSelectionHandler>()
        val menuItem = NodeSelectionModeMenuItem(
            action = menuAction,
            handler = mockHandler
        )
        val result = underTest(menuItem)

        assertThat(result).isEqualTo(expectedAction)
    }

    @Test
    fun `test that mapper throws IllegalArgumentException for unknown action type`() {
        val unknownAction = mock<MenuActionWithIcon>()
        val mockHandler = mock<NodeSelectionHandler>()
        val menuItem = NodeSelectionModeMenuItem(
            action = unknownAction,
            handler = mockHandler
        )

        assertThat(underTest(menuItem)).isNull()
    }

    companion object {
        @JvmStatic
        fun provideMenuActionMappings(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    mock<CopyMenuAction>(),
                    NodeSelectionAction.Copy,
                ),
                Arguments.of(
                    mock<MoveMenuAction>(),
                    NodeSelectionAction.Move,
                ),
                Arguments.of(
                    mock<HideMenuAction>(),
                    NodeSelectionAction.Hide,
                ),
                Arguments.of(
                    mock<TrashMenuAction>(),
                    NodeSelectionAction.RubbishBin,
                ),
                Arguments.of(
                    mock<DownloadMenuAction>(),
                    NodeSelectionAction.Download,
                ),
                Arguments.of(
                    mock<ManageLinkMenuAction>(),
                    NodeSelectionAction.ShareLink(2),
                )
            )
        }
    }
}
