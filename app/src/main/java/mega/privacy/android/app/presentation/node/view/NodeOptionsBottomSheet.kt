package mega.privacy.android.app.presentation.node.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.node.NodeBottomSheetActionHandler
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.domain.entity.node.TypedNode


/**
 * Node options bottom sheet
 *
 * @param modalSheetState
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun NodeOptionsBottomSheet(
    modalSheetState: ModalBottomSheetState,
    node: NodeUIItem<TypedNode>,
    handler: NodeBottomSheetActionHandler,
    viewModel: NodeOptionsBottomSheetViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {
    viewModel.getBottomSheetOptions(node)
    val state by viewModel.state.collectAsStateWithLifecycle()
    BottomSheet(
        modalSheetState = modalSheetState,
        sheetHeader = { Text(text = state.name) },
        sheetBody = {
            LazyColumn {
                state.actions.groupBy {
                    it.group
                }.toSortedMap()
                    .mapValues { (_, list) ->
                        list.sortedBy { it.orderInGroup }
                    }
                    .values
                    .forEachIndexed { index, actions ->
                        items(actions) { item: BottomSheetMenuItem ->
                            item.control(onDismiss, handler::handleAction)
                        }

                        if (index < state.actions.size - 1) {
                            item {
                                Divider(
                                    modifier = Modifier
                                        .padding(start = 72.dp)
                                        .testTag("$DIVIDER_TAG$index")
                                )
                            }
                        }
                    }
            }
        },
    )
}

internal const val DIVIDER_TAG = "node_options_bottom_sheet:divider"
