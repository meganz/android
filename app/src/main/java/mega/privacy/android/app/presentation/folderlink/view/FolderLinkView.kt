package mega.privacy.android.app.presentation.folderlink.view

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ads.AdsContainer
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogView
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.filelink.view.ImportDownloadView
import mega.privacy.android.app.presentation.folderlink.model.FolderLinkState
import mega.privacy.android.app.presentation.folderlink.model.LinkErrorState
import mega.privacy.android.app.presentation.folderlink.view.Constants.APPBAR_MORE_OPTION_TAG
import mega.privacy.android.app.presentation.folderlink.view.Constants.IMPORT_BUTTON_TAG
import mega.privacy.android.app.presentation.folderlink.view.Constants.SAVE_BUTTON_TAG
import mega.privacy.android.app.presentation.search.SEARCH_SCREEN_MINI_AUDIO_PLAYER_TEST_TAG
import mega.privacy.android.app.presentation.search.view.LoadingStateView
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.app.presentation.transfers.TransferManagementUiState
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.buttons.DebouncedButtonContainer
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.widgets.TransfersWidgetViewAnimated
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.accent_900_accent_050
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_020_grey_700
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR


/**
 * Main view of FolderLinkActivity
 */
@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
internal fun FolderLinkView(
    state: FolderLinkState,
    transferState: TransferManagementUiState,
    scaffoldState: ScaffoldState,
    onBackPressed: () -> Unit,
    onShareClicked: () -> Unit,
    onMoreOptionClick: (NodeUIItem<TypedNode>?) -> Unit,
    onItemClicked: (NodeUIItem<TypedNode>) -> Unit,
    onLongClick: (NodeUIItem<TypedNode>) -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onSortOrderClick: () -> Unit,
    onSelectAllActionClicked: () -> Unit,
    onClearAllActionClicked: () -> Unit,
    onSaveToDeviceClicked: (NodeUIItem<TypedNode>?) -> Unit,
    onImportClicked: (NodeUIItem<TypedNode>?) -> Unit,
    onOpenFile: (Intent) -> Unit,
    onResetOpenFile: () -> Unit,
    onSelectImportLocation: () -> Unit,
    onResetSelectImportLocation: () -> Unit,
    onResetSnackbarMessage: () -> Unit,
    onResetMoreOptionNode: () -> Unit,
    onResetOpenMoreOption: () -> Unit,
    onStorageStatusDialogDismiss: () -> Unit,
    onStorageDialogActionButtonClick: () -> Unit,
    onStorageDialogAchievementButtonClick: () -> Unit,
    emptyViewString: String,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit,
    onEnterMediaDiscoveryClick: () -> Unit,
    onTransferWidgetClick: () -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    request: AdManagerAdRequest?,
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )

    val firstItemVisible by remember {
        derivedStateOf {
            if (state.currentViewType == ViewType.LIST)
                listState.firstVisibleItemIndex == 0
            else
                gridState.firstVisibleItemIndex == 0
        }
    }
    val title =
        if (!state.isNodesFetched) "MEGA" else state.title

    BackHandler(enabled = modalSheetState.isVisible) {
        coroutineScope.launch { modalSheetState.hide() }
    }

    EventEffect(
        event = state.openFile,
        onConsumed = onResetOpenFile,
        action = onOpenFile
    )

    EventEffect(
        event = state.selectImportLocation,
        onConsumed = onResetSelectImportLocation,
        action = onSelectImportLocation
    )

    EventEffect(
        event = state.openMoreOption,
        onConsumed = onResetOpenMoreOption,
        action = { modalSheetState.show() }
    )

    EventEffect(event = state.snackbarMessageContent, onConsumed = onResetSnackbarMessage) {
        scaffoldState.snackbarHostState.showAutoDurationSnackbar(it)
    }

    LaunchedEffect(modalSheetState.isVisible) {
        if (!modalSheetState.isVisible)
            onResetMoreOptionNode()
    }
    FolderLinkBottomSheetView(
        modalSheetState = modalSheetState,
        coroutineScope = coroutineScope,
        nodeUIItem = state.moreOptionNode,
        showImport = state.hasDbCredentials,
        onImportClicked = onImportClicked,
        onSaveToDeviceClicked = onSaveToDeviceClicked,
    ) {
        MegaScaffold(
            scaffoldState = scaffoldState,
            topBar = {
                if (state.selectedNodeCount > 0) {
                    FolderLinkSelectedTopAppBar(
                        title = state.selectedNodeCount.toString(),
                        elevation = !firstItemVisible,
                        onBackPressed = onBackPressed,
                        onSelectAllClicked = onSelectAllActionClicked,
                        onClearAllClicked = onClearAllActionClicked,
                        onSaveToDeviceClicked = { onSaveToDeviceClicked(null) }
                    )
                } else {
                    FolderLinkTopAppBar(
                        title = title,
                        showMenuActions = state.showContentActions,
                        elevation = !firstItemVisible,
                        onBackPressed = onBackPressed,
                        onShareClicked = onShareClicked,
                        onMoreClicked = { onMoreOptionClick(null) }
                    )
                }
            },
            floatingActionButton = {
                if (!transferState.hideTransfersWidget) {
                    TransfersWidgetViewAnimated(
                        transfersInfo = transferState.transfersInfo,
                        onClick = onTransferWidgetClick,
                    )
                }
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (state.showContentActions) {
                        ImportDownloadView(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(MaterialTheme.colors.grey_020_grey_700),
                            hasDbCredentials = state.hasDbCredentials,
                            onImportClicked = onImportClicked,
                            onSaveToDeviceClicked = { onSaveToDeviceClicked(null) }
                        )
                    }
                    request?.let {
                        AdsContainer(
                            request = request,
                            modifier = Modifier.fillMaxWidth(),
                            isLoggedInUser = state.hasDbCredentials,
                        )
                    }
                }
            },
        ) { paddingValues ->
            ConstraintLayout(
                modifier = Modifier
                    .background(MaterialTheme.colors.background)
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                val (audioPlayer, folderLinkView) = createRefs()
                MiniAudioPlayerView(
                    modifier = Modifier
                        .constrainAs(audioPlayer) {
                            bottom.linkTo(parent.bottom)
                        }
                        .fillMaxWidth()
                        .testTag(SEARCH_SCREEN_MINI_AUDIO_PLAYER_TEST_TAG),
                    lifecycle = LocalLifecycleOwner.current.lifecycle,
                )

                Box(
                    modifier = Modifier
                        .constrainAs(folderLinkView) {
                            top.linkTo(parent.top)
                            bottom.linkTo(audioPlayer.top)
                            height = Dimension.fillToConstraints
                        }
                        .fillMaxWidth()
                ) {
                    when {
                        state.isLoading -> {
                            LoadingStateView(isList = true)
                        }

                        state.errorState == LinkErrorState.Expired -> {
                            ExpiredLinkView(
                                title = sharedR.string.folder_link_expired_title
                            )
                        }

                        state.errorState == LinkErrorState.Unavailable -> {
                            UnavailableLinkView(
                                title = sharedR.string.folder_link_unavailable_title,
                                subtitle = sharedR.string.general_link_unavailable_subtitle,
                                bulletPoints = listOf(
                                    sharedR.string.folder_link_unavailable_deleted,
                                    sharedR.string.folder_link_unavailable_disabled,
                                    sharedR.string.general_link_unavailable_invalid_url,
                                    R.string.folder_link_unavaible_ToS_violation
                                )
                            )
                        }

                        state.nodesList.isEmpty() -> {
                            EmptyFolderLinkView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(horizontal = 8.dp),
                                emptyViewString = emptyViewString,
                            )
                        }

                        else -> {
                            NodesView(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp),
                                nodeUIItems = state.nodesList,
                                onMenuClick = onMoreOptionClick,
                                onItemClicked = onItemClicked,
                                onLongClick = onLongClick,
                                sortOrder = "",
                                isListView = state.currentViewType == ViewType.LIST,
                                onSortOrderClick = onSortOrderClick,
                                onChangeViewTypeClick = onChangeViewTypeClick,
                                showSortOrder = false,
                                listState = listState,
                                gridState = gridState,
                                onLinkClicked = onLinkClicked,
                                onDisputeTakeDownClicked = onDisputeTakeDownClicked,
                                showMediaDiscoveryButton = state.hasMediaItem,
                                onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
                                isPublicNode = true,
                                fileTypeIconMapper = fileTypeIconMapper,
                                inSelectionMode = state.selectedNodeCount > 0
                            )
                        }
                    }

                    if (state.storageStatusDialogState != null) {
                        StorageStatusDialogView(
                            state = state.storageStatusDialogState,
                            dismissClickListener = onStorageStatusDialogDismiss,
                            actionButtonClickListener = onStorageDialogActionButtonClick,
                            achievementButtonClickListener = onStorageDialogAchievementButtonClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FolderLinkTopAppBar(
    title: String,
    showMenuActions: Boolean,
    elevation: Boolean,
    onBackPressed: () -> Unit,
    onShareClicked: () -> Unit,
    onMoreClicked: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.general_back_button),
                    tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White
                )
            }
        },
        actions = {
            if (showMenuActions) {
                IconButton(onClick = onShareClicked) {
                    Image(
                        painter = rememberVectorPainter(IconPack.Medium.Regular.Outline.ShareNetwork),
                        contentDescription = stringResource(id = R.string.general_share),
                        colorFilter = ColorFilter.tint(if (MaterialTheme.colors.isLight) Color.Black else Color.White)
                    )
                }

                IconButton(
                    modifier = Modifier.testTag(APPBAR_MORE_OPTION_TAG),
                    onClick = onMoreClicked
                ) {
                    Icon(
                        IconPack.Medium.Regular.Outline.MoreVertical,
                        contentDescription = stringResource(id = R.string.label_more),
                        tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White
                    )
                }
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp
    )
}

@Composable
internal fun FolderLinkSelectedTopAppBar(
    title: String,
    elevation: Boolean,
    onBackPressed: () -> Unit,
    onSelectAllClicked: () -> Unit,
    onClearAllClicked: () -> Unit,
    onSaveToDeviceClicked: () -> Unit,
) {

    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.accent_900_accent_050
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.general_back_button),
                    tint = MaterialTheme.colors.accent_900_accent_050
                )
            }
        },
        actions = {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.label_more),
                    tint = MaterialTheme.colors.accent_900_accent_050
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(onClick = { onSelectAllClicked() }) { Text(stringResource(id = R.string.action_select_all)) }
                DropdownMenuItem(onClick = { onClearAllClicked() }) { Text(stringResource(id = R.string.action_unselect_all)) }
                DropdownMenuItem(onClick = { onSaveToDeviceClicked() }) { Text(stringResource(id = R.string.general_save_to_device)) }
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp
    )
}

@Composable
internal fun ImportDownloadView(
    modifier: Modifier,
    hasDbCredentials: Boolean,
    onImportClicked: (NodeUIItem<TypedNode>?) -> Unit,
    onSaveToDeviceClicked: () -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.End) {
        if (hasDbCredentials) {
            TextMegaButton(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .testTag(IMPORT_BUTTON_TAG),
                textId = R.string.add_to_cloud,
                onClick = {
                    onImportClicked(null)
                },
            )
        }
        DebouncedButtonContainer(onSaveToDeviceClicked) { isClickAllowed, debouncedOnClick ->
            TextMegaButton(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .testTag(SAVE_BUTTON_TAG),
                textId = R.string.general_save_to_device,
                onClick = debouncedOnClick,
                enabled = isClickAllowed,
            )
        }
    }
}

@Composable
internal fun EmptyFolderLinkView(
    modifier: Modifier,
    emptyViewString: String,
) {
    val imageResource = iconPackR.drawable.ic_empty_folder_glass
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(id = imageResource), contentDescription = "Folder")
        Text(
            text = emptyViewString,
            style = MaterialTheme.typography.subtitle1,
            color = if (MaterialTheme.colors.isLight) Color.Black else Color.White
        )
    }
}


internal object Constants {

    /**
     * Test tag for save to device bottom sheet button
     * @see FolderLinkBottomSheetView
     */
    const val BOTTOM_SHEET_SAVE = "bottom_sheet_save"

    /**
     * Test tag for import bottom sheet button
     * @see FolderLinkBottomSheetView
     */
    const val BOTTOM_SHEET_IMPORT = "bottom_sheet_import"

    /**
     * Test tag for save to import button
     */
    const val IMPORT_BUTTON_TAG = "import_button_tag"

    /**
     * Test tag for save to device button
     */
    const val SAVE_BUTTON_TAG = "save_button_tag"

    /**
     * Test tag for app bar more option
     */
    const val APPBAR_MORE_OPTION_TAG = "appbar_more_option_tag"
}

@CombinedThemePreviews
@Composable
private fun FolderLinkTopAppBarPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FolderLinkTopAppBar(
            title = "Folder Name",
            showMenuActions = false,
            elevation = false,
            onBackPressed = {},
            onShareClicked = {},
            onMoreClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun EmptyFolderLinkViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaScaffold(
            bottomBar = {
                ImportDownloadView(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(MaterialTheme.colors.grey_020_grey_700),
                    hasDbCredentials = true,
                    onImportClicked = { },
                    onSaveToDeviceClicked = { }
                )
            }
        ) {
            EmptyFolderLinkView(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                emptyViewString = stringResource(id = R.string.file_browser_empty_folder),
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun FolderLinkSelectedTopAppBarPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FolderLinkSelectedTopAppBar(
            title = "Folder Name",
            elevation = false,
            onBackPressed = {},
            onSelectAllClicked = {},
            onClearAllClicked = {},
            onSaveToDeviceClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ImportDownloadViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ImportDownloadView(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colors.grey_020_grey_700),
            hasDbCredentials = true,
            onImportClicked = {},
            onSaveToDeviceClicked = {}
        )
    }
}