package mega.privacy.android.app.presentation.node.model.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.BottomSheetClickHandler
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.android.core.ui.model.menu.MenuActionWithIcon
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class NodeBottomSheetActionMapperTest {
    private lateinit var underTest: NodeBottomSheetActionMapper

    @BeforeEach
    internal fun setUp() {
        underTest = NodeBottomSheetActionMapper()
    }

    @Test
    fun `test that group id matches menu item group id`() = runTest {
        val expectedGroup = 22
        val mockBuildComposeControl = mock<BottomSheetClickHandler>()
        val bottomSheetMenuItem = mock<NodeBottomSheetMenuItem<*>> {
            on { menuAction }.thenReturn(getMenuAction(10))
            on { groupId }.thenReturn(expectedGroup)
            on { buildComposeControl(any()) }.thenReturn(mockBuildComposeControl)
            onBlocking { shouldDisplay(any(), any(), any(), any(), any()) }.thenReturn(true)
        }
        val actual = underTest(
            toolbarOptions = setOf(
                element = bottomSheetMenuItem,
            ),
            selectedNode = mock<TypedFileNode>(),
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackUps = false,
            isConnected = true
        )

        assertThat(actual.first().group).isEqualTo(expectedGroup)
    }

    @Test
    fun `test that the order id matches the order in category id`() = runTest {
        val expectedOrderInGroup = 55
        val mockBuildComposeControl = mock<BottomSheetClickHandler>()
        val bottomSheetMenuItem = mock<NodeBottomSheetMenuItem<*>> {
            on { menuAction }.thenReturn(getMenuAction(expectedOrderInGroup))
            on { groupId }.thenReturn(10)
            on { buildComposeControl(any()) }.thenReturn(mockBuildComposeControl)
            onBlocking { shouldDisplay(any(), any(), any(), any(), any()) }.thenReturn(true)
        }
        val actual = underTest(
            toolbarOptions = setOf(
                element = bottomSheetMenuItem,
            ),
            selectedNode = mock<TypedFileNode>(),
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackUps = false,
            isConnected = true
        )

        assertThat(actual.first().orderInGroup).isEqualTo(expectedOrderInGroup)
    }

    @Test
    fun `test that the build compose control function is called`() = runTest {
        val mockBuildComposeControl = mock<BottomSheetClickHandler>()
        val bottomSheetMenuItem = mock<NodeBottomSheetMenuItem<*>> {
            on { menuAction }.thenReturn(getMenuAction(10))
            on { groupId }.thenReturn(10)
            on { buildComposeControl(any()) }.thenReturn(mockBuildComposeControl)
            onBlocking { shouldDisplay(any(), any(), any(), any(), any()) }.thenReturn(true)
        }
        val selectedNode = mock<TypedFileNode>()
        underTest(
            toolbarOptions = setOf(
                element = bottomSheetMenuItem,
            ),
            selectedNode = selectedNode,
            isNodeInRubbish = false,
            accessPermission = AccessPermission.OWNER,
            isInBackUps = false,
            isConnected = true
        )

        verify(bottomSheetMenuItem).buildComposeControl(selectedNode)
    }

    private fun getMenuAction(orderInCategory: Int) =
        object : MenuActionWithIcon {
            @Composable
            override fun getIconPainter(): Painter = painterResource(id = 1)

            @Composable
            override fun getDescription(): String = "description"

            override val testTag: String
                get() = "test_tag"

            override val orderInCategory: Int
                get() = orderInCategory
        }
}