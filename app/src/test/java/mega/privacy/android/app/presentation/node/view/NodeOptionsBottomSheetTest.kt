package mega.privacy.android.app.presentation.node.view

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.presentation.node.NodeBottomSheetActionHandler
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.node.model.NodeBottomSheetState
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class NodeOptionsBottomSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mock<NodeOptionsBottomSheetViewModel>()
    private val nodeBottomSheetActionHandler = mock<NodeBottomSheetActionHandler>()
    private val sampleNode = mock<TypedFolderNode> {
        on { name }.thenReturn("name")
    }
    private val navHostController = mock<NavHostController>()

    @Test
    fun `test that a single group has no dividers`() {
        viewModel.stub {
            on { state }.thenReturn(MutableStateFlow(
                NodeBottomSheetState(
                    name = "",
                    isOnline = true,
                    actions = listOf(
                        BottomSheetMenuItem(
                            group = 1,
                            orderInGroup = 1,
                            control = { _, _, _ -> }
                        )
                    ),
                ),
            )
            )
        }

        composeTestRule.setContent {
            NodeOptionsBottomSheet(
                modalSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Expanded),
                node = sampleNode,
                handler = nodeBottomSheetActionHandler,
                viewModel = viewModel,
                navHostController = navHostController,
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithTag(DIVIDER_TAG + 0).assertDoesNotExist()
    }

    @Test
    fun `test that two groups have a divider`() {
        viewModel.stub {
            on { state }.thenReturn(MutableStateFlow(
                NodeBottomSheetState(
                    name = "",
                    isOnline = true,
                    actions = listOf(
                        BottomSheetMenuItem(
                            group = 1,
                            orderInGroup = 1,
                            control = { _, _, _ -> }
                        ),
                        BottomSheetMenuItem(
                            group = 2,
                            orderInGroup = 1,
                            control = { _, _, _ -> }
                        )
                    ),
                ),
            )
            )
        }

        composeTestRule.setContent {
            NodeOptionsBottomSheet(
                modalSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Expanded),
                node = sampleNode,
                handler = nodeBottomSheetActionHandler,
                viewModel = viewModel,
                navHostController = navHostController,
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag(DIVIDER_TAG + 0).assertExists()
    }
}