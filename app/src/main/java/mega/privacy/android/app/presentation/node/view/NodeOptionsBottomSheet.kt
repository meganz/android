package mega.privacy.android.app.presentation.node.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.presentation.node.NodeBottomSheetActionHandler
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.view.extension.fileInfo
import mega.privacy.android.app.presentation.view.extension.folderInfo
import mega.privacy.android.app.presentation.view.extension.getIcon
import mega.privacy.android.core.ui.controls.dividers.DividerSpacing
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import timber.log.Timber


/**
 * Node options bottom sheet
 */
@Composable
internal fun NodeOptionsBottomSheetContent(
    handler: NodeBottomSheetActionHandler,
    navHostController: NavHostController,
    onDismiss: () -> Unit,
    viewModel: NodeOptionsBottomSheetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val node: TypedNode? = uiState.node

    val sortedMap = remember(node?.id?.longValue) {
        mutableStateOf(
            uiState.actions
                .groupBy { it.group }
                .toSortedMap()
                .mapValues { (_, list) ->
                    list.sortedBy { it.orderInGroup }
                }.values
        )
    }
    EventEffect(
        event = uiState.error,
        onConsumed = viewModel::onConsumeErrorState,
        action = {
            Timber.e(it)
            onDismiss()
        },
    )

    NodeListViewItem(
        title = node?.name.orEmpty(),
        titleColor = if (node?.isTakenDown == true) TextColor.Error else TextColor.Primary,
        titleOverflow = LongTextBehaviour.MiddleEllipsis,
        subtitle = when (node) {
            is FileNode -> node.fileInfo()
            is FolderNode -> node.folderInfo()
            else -> ""
        },
        showVersion = node?.hasVersion == true,
        icon = (node as? TypedFolderNode)?.getIcon()
            ?: MimeTypeList.typeForName(node?.name).iconResourceId,
        thumbnailData = node?.id?.let { ThumbnailRequest(it) },
    )
    MegaDivider(dividerSpacing = DividerSpacing.StartSmall)
    LazyColumn {
        sortedMap.value
            .forEachIndexed { index, actions ->
                items(actions) { item: BottomSheetMenuItem ->
                    item.control(onDismiss, handler::handleAction, navHostController)
                }

                if (index < uiState.actions.size - 1 && index != sortedMap.value.size - 1) {
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
}

internal const val DIVIDER_TAG = "node_options_bottom_sheet:divider"
