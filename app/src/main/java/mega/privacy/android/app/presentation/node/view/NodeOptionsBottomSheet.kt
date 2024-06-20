package mega.privacy.android.app.presentation.node.view

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.view.extension.fileInfo
import mega.privacy.android.app.presentation.view.extension.folderInfo
import mega.privacy.android.app.presentation.view.extension.getIcon
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import timber.log.Timber


/**
 * Node options bottom sheet
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NodeOptionsBottomSheetContent(
    handler: NodeActionHandler,
    navHostController: NavHostController,
    onDismiss: () -> Unit,
    nodeId: Long,
    nodeSourceType: NodeSourceType,
    fileTypeIconMapper: FileTypeIconMapper,
    viewModel: NodeOptionsBottomSheetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val node: TypedNode? = uiState.node
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        keyboardController?.hide()
        viewModel.getBottomSheetOptions(nodeId, nodeSourceType)
    }

    val sortedMap = remember(uiState.actions) {
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

    @Composable
    fun getOutShareInfo(): String? = when {
        uiState.outgoingShares.isEmpty() -> null
        uiState.outgoingShares.size == 1 -> uiState.outgoingShares[0].user
        else -> pluralStringResource(
            R.plurals.general_num_shared_with,
            uiState.outgoingShares.size,
            uiState.outgoingShares.size,
        )
    }

    if (uiState.node != null) {
        NodeListViewItem(
            modifier = Modifier.semantics { testTagsAsResourceId = true },
            title = node?.name.orEmpty(),
            titleColor = if (node?.isTakenDown == true) TextColor.Error else TextColor.Primary,
            titleOverflow = LongTextBehaviour.MiddleEllipsis,
            subtitle = uiState.shareInfo ?: getOutShareInfo() ?: when (node) {
                is FileNode -> node.fileInfo()
                is FolderNode -> node.folderInfo()
                else -> ""
            },
            showVersion = node?.hasVersion == true,
            icon = node?.getIcon(fileTypeIconMapper = fileTypeIconMapper)
                ?: iconPackR.drawable.ic_generic_medium_solid,
            thumbnailData = node?.id?.let { ThumbnailRequest(it) },
            accessPermissionIcon = uiState.accessPermissionIcon,
        )
    }
    MegaDivider(dividerType = DividerType.SmallStartPadding)
    LazyColumn(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
        sortedMap.value
            .forEachIndexed { index, actions ->
                items(actions) { item: BottomSheetMenuItem ->
                    item.control(
                        onDismiss,
                        handler::handleAction,
                        navHostController,
                        coroutineScope
                    )
                }

                if (index < uiState.actions.size - 1 && index != sortedMap.value.size - 1) {
                    item {
                        MegaDivider(
                            dividerType = DividerType.BigStartPadding,
                            modifier = Modifier.testTag("$DIVIDER_TAG$index")
                        )
                    }
                }
            }
    }


}

internal const val DIVIDER_TAG = "node_options_bottom_sheet:divider"
