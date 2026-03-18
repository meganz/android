package mega.privacy.android.app.presentation.audiosection.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.audiosection.AudioSectionViewModel
import mega.privacy.android.app.presentation.audiosection.model.AudioUiEntity
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.search.model.navigation.removeNodeLinkDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.cannotVerifyUserNavigation
import mega.privacy.android.app.presentation.search.navigation.changeLabelBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.changeNodeExtensionDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.foreignNodeDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.leaveFolderShareDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.moveToRubbishOrDeleteNavigation
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetRoute
import mega.privacy.android.app.presentation.search.navigation.overQuotaDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.removeShareFolderDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.renameDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.shareFolderAccessDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.shareFolderDialogNavigation
import mega.privacy.android.app.presentation.search.view.LoadingStateView
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetLayout

@Composable
fun AudioSectionComposeScreen(
    navHostController: NavHostController,
    onSortOrderClick: () -> Unit,
    listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
    nodeActionHandler: NodeActionHandler,
    fileTypeIconMapper: FileTypeIconMapper,
    bottomSheetNavigator: BottomSheetNavigator,
    modifier: Modifier = Modifier,
    viewModel: AudioSectionViewModel = hiltViewModel(),
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    MegaBottomSheetLayout(
        modifier = modifier,
        bottomSheetNavigator = bottomSheetNavigator,
    ) {
        NavHost(
            modifier = modifier,
            navController = navHostController,
            startDestination = audioSectionRoute
        ) {
            composable(
                route = audioSectionRoute
            ) {
                AudioSectionComposeView(
                    viewModel = viewModel,
                    onChangeViewTypeClick = viewModel::onChangeViewTypeClicked,
                    onSortOrderClick = onSortOrderClick,
                    onLongClick = { item, index ->
                        viewModel.onItemLongClicked(item, index)
                    },
                    onMenuClick = { item ->
                        val type = NodeSourceType.AUDIO
                        keyboardController?.hide()
                        navHostController.navigate(
                            route = nodeBottomSheetRoute.plus("/${item.id.longValue}")
                                .plus("/${type.name}")
                        ) {
                            popUpTo(nodeBottomSheetRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            moveToRubbishOrDeleteNavigation(
                navHostController = navHostController,
                listToStringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
            renameDialogNavigation(navHostController = navHostController)
            nodeBottomSheetNavigation(
                nodeActionHandler = nodeActionHandler,
                navHostController = navHostController,
                fileTypeIconMapper = fileTypeIconMapper
            )
            changeLabelBottomSheetNavigation(navHostController)
            changeNodeExtensionDialogNavigation(navHostController)
            cannotVerifyUserNavigation(navHostController)
            removeNodeLinkDialogNavigation(
                navHostController = navHostController,
                listToStringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
            shareFolderDialogNavigation(
                navHostController = navHostController,
                nodeActionHandler = nodeActionHandler,
                stringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
            removeShareFolderDialogNavigation(
                navHostController = navHostController,
                stringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
            leaveFolderShareDialogNavigation(
                navHostController = navHostController,
                stringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
            overQuotaDialogNavigation(navHostController = navHostController)
            foreignNodeDialogNavigation(navHostController = navHostController)
            shareFolderAccessDialogNavigation(
                navHostController = navHostController,
                listToStringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
        }
    }
}

/**
 * The compose view for audio section
 */
@Composable
fun AudioSectionComposeView(
    viewModel: AudioSectionViewModel,
    modifier: Modifier = Modifier,
    onChangeViewTypeClick: () -> Unit = {},
    onSortOrderClick: () -> Unit = {},
    onMenuClick: (AudioUiEntity) -> Unit = {},
    onLongClick: (item: AudioUiEntity, index: Int) -> Unit = { _, _ -> },
) {

    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val progressBarShowing = uiState.progressBarShowing
    val items = uiState.allAudios
    val scrollToTop = uiState.scrollToTop

    LaunchedEffect(items) {
        if (scrollToTop) {
            if (uiState.currentViewType == ViewType.LIST)
                listState.scrollToItem(0)
            else
                gridState.scrollToItem(0)
        }
    }
    Box(modifier = modifier) {
        when {
            progressBarShowing -> LoadingStateView(uiState.currentViewType == ViewType.LIST)

            items.isEmpty() -> LegacyMegaEmptyView(
                modifier = Modifier,
                text = stringResource(id = R.string.homepage_empty_hint_audio),
                imagePainter = painterResource(id = iconPackR.drawable.ic_audio_glass)
            )

            else -> {
                AudiosView(
                    items = items,
                    shouldApplySensitiveMode = uiState.hiddenNodeEnabled
                            && uiState.accountType?.isPaid == true
                            && !uiState.isBusinessAccountExpired,
                    isListView = uiState.currentViewType == ViewType.LIST,
                    listState = listState,
                    gridState = gridState,
                    sortOrder = stringResource(
                        id = SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                            ?: R.string.sortby_name
                    ),
                    modifier = Modifier,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    onSortOrderClick = onSortOrderClick,
                    onClick = viewModel::onItemClicked,
                    onLongClick = onLongClick,
                    onMenuClick = onMenuClick,
                    inSelectionMode = uiState.isInSelection
                )
            }
        }
    }
}

internal const val audioSectionRoute = "audioSection/audio_section"