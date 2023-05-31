package mega.privacy.android.app.presentation.folderlink.view

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.favourites.ThumbnailViewModel
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.folderlink.model.FolderLinkState
import mega.privacy.android.app.presentation.folderlink.view.Constants.APPBAR_MORE_OPTION_TAG
import mega.privacy.android.app.presentation.folderlink.view.Constants.IMPORT_BUTTON_TAG
import mega.privacy.android.app.presentation.folderlink.view.Constants.SAVE_BUTTON_TAG
import mega.privacy.android.app.presentation.folderlink.view.Constants.SNACKBAR_TAG
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_700
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaNode

internal object Constants {
    /**
     * Test tag for message SnackBar
     */
    const val SNACKBAR_TAG = "snackbar_test_tag"

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

/**
 * Main view of FolderLinkActivity
 */
@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
internal fun FolderLinkView(
    state: FolderLinkState,
    onBackPressed: () -> Unit,
    onShareClicked: () -> Unit,
    stringUtilWrapper: StringUtilWrapper,
    onMoreOptionClick: (NodeUIItem?) -> Unit,
    onItemClicked: (NodeUIItem) -> Unit,
    onLongClick: (NodeUIItem) -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onSortOrderClick: () -> Unit,
    onSelectAllActionClicked: () -> Unit,
    onClearAllActionClicked: () -> Unit,
    onSaveToDeviceClicked: (NodeUIItem?) -> Unit,
    onImportClicked: (NodeUIItem?) -> Unit,
    onOpenFile: (Intent) -> Unit,
    onResetOpenFile: () -> Unit,
    onDownloadNode: (List<MegaNode>) -> Unit,
    onResetDownloadNode: () -> Unit,
    onSelectImportLocation: () -> Unit,
    onResetSelectImportLocation: () -> Unit,
    onResetSnackbarMessage: () -> Unit,
    onResetMoreOptionNode: () -> Unit,
    onResetOpenMoreOption: () -> Unit,
    emptyViewString: String,
    thumbnailViewModel: ThumbnailViewModel,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scaffoldState = rememberScaffoldState()
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded },
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
        if (!state.isNodesFetched) "MEGA - ${stringResource(id = R.string.general_loading)}" else state.title

    BackHandler(enabled = modalSheetState.isVisible) {
        coroutineScope.launch { modalSheetState.hide() }
    }

    EventEffect(
        event = state.openFile,
        onConsumed = onResetOpenFile,
        action = onOpenFile
    )

    EventEffect(
        event = state.downloadNodes,
        onConsumed = onResetDownloadNode,
        action = onDownloadNode
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
        snackBarHostState.showSnackbar(it)
    }

    LaunchedEffect(modalSheetState.isVisible) {
        if (!modalSheetState.isVisible)
            onResetMoreOptionNode()
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            if (state.selectedNodeCount > 0) {
                FolderLinkSelectedTopAppBar(
                    title = title,
                    elevation = !firstItemVisible,
                    onBackPressed = onBackPressed,
                    onSelectAllClicked = onSelectAllActionClicked,
                    onClearAllClicked = onClearAllActionClicked,
                    onSaveToDeviceClicked = { onSaveToDeviceClicked(null) }
                )
            } else {
                FolderLinkTopAppBar(
                    title = title,
                    elevation = !firstItemVisible,
                    onBackPressed = onBackPressed,
                    onShareClicked = onShareClicked,
                    onMoreClicked = { onMoreOptionClick(null) }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                Snackbar(
                    modifier = Modifier.testTag(SNACKBAR_TAG),
                    snackbarData = data,
                    backgroundColor = black.takeIf { MaterialTheme.colors.isLight } ?: white
                )
            }
        }
    ) {
        if (state.nodesList.isEmpty()) {
            EmptyFolderLinkView(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                emptyViewString = emptyViewString,
                state.isNodesFetched
            )
        } else {
            Column {
                NodesView(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    nodeUIItems = state.nodesList,
                    stringUtilWrapper = stringUtilWrapper,
                    onMenuClick = { onMoreOptionClick(it) },
                    onItemClicked = onItemClicked,
                    onLongClick = onLongClick,
                    sortOrder = "",
                    isListView = state.currentViewType == ViewType.LIST,
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    showSortOrder = false,
                    listState = listState,
                    gridState = gridState,
                    thumbnailViewModel = thumbnailViewModel,
                    onLinkClicked = onLinkClicked,
                    onDisputeTakeDownClicked = onDisputeTakeDownClicked
                )
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
        }

        FolderLinkBottomSheetView(
            modalSheetState = modalSheetState,
            coroutineScope = coroutineScope,
            nodeUIItem = state.moreOptionNode,
            showImport = state.hasDbCredentials,
            onImportClicked = onImportClicked,
            onSaveToDeviceClicked = onSaveToDeviceClicked
        )
    }
}

@Composable
internal fun FolderLinkTopAppBar(
    title: String,
    elevation: Boolean,
    onBackPressed: () -> Unit,
    onShareClicked: () -> Unit,
    onMoreClicked: () -> Unit
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
            IconButton(onClick = onShareClicked) {
                Image(
                    painter = painterResource(id = R.drawable.ic_social_share_white),
                    contentDescription = stringResource(id = R.string.general_share),
                    colorFilter = ColorFilter.tint(if (MaterialTheme.colors.isLight) Color.Black else Color.White)
                )
            }

            IconButton(
                modifier = Modifier.testTag(APPBAR_MORE_OPTION_TAG),
                onClick = onMoreClicked
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.label_more),
                    tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White
                )
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
    onSaveToDeviceClicked: () -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

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
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.label_more),
                    tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White
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
    onImportClicked: (NodeUIItem?) -> Unit,
    onSaveToDeviceClicked: () -> Unit
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.End) {
        if (hasDbCredentials) {
            TextMegaButton(
                modifier = Modifier.padding(end = 16.dp).testTag(IMPORT_BUTTON_TAG),
                textId = R.string.add_to_cloud,
                onClick = { onImportClicked(null) }
            )
        }
        TextMegaButton(
            modifier = Modifier.padding(end = 16.dp).testTag(SAVE_BUTTON_TAG),
            textId = R.string.general_save_to_device,
            onClick = onSaveToDeviceClicked
        )
    }
}

@Composable
internal fun EmptyFolderLinkView(
    modifier: Modifier,
    emptyViewString: String,
    isNodesFetched: Boolean
) {
    val orientation = LocalConfiguration.current.orientation
    val imageResource = if (orientation == Configuration.ORIENTATION_LANDSCAPE)
        R.drawable.ic_zero_landscape_empty_folder
    else
        R.drawable.ic_zero_portrait_empty_folder
    if (isNodesFetched) {
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
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkSimpleAppBarPreview")
@Composable
private fun PreviewFolderLinkTopAppBar() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FolderLinkTopAppBar(
            title = "Folder Name",
            elevation = false,
            onBackPressed = {},
            onShareClicked = {},
            onMoreClicked = {}
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkSimpleAppBarPreview")
@Composable
private fun PreviewEmptyFolderLinkView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        EmptyFolderLinkView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            emptyViewString = stringResource(id = R.string.file_browser_empty_folder),
            isNodesFetched = true
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkSimpleAppBarPreview")
@Composable
private fun PreviewFolderLinkSelectedTopAppBar() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkSimpleAppBarPreview")
@Composable
private fun PreviewImportDownloadView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
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