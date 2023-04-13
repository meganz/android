package mega.privacy.android.app.presentation.folderlink.view

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.folderlink.model.FolderLinkState
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * Main view of FolderLinkActivity
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
internal fun FolderLinkView(
    state: FolderLinkState,
    onBackPressed: () -> Unit,
    onShareClicked: () -> Unit,
    onMoreClicked: () -> Unit,
    stringUtilWrapper: StringUtilWrapper,
    onMenuClick: (NodeUIItem) -> Unit,
    onItemClicked: (NodeUIItem) -> Unit,
    onLongClick: (NodeUIItem) -> Unit,
    onChangeViewTypeClick: () -> Unit,
    onSortOrderClick: () -> Unit,
    onSelectAllActionClicked: () -> Unit,
    onClearAllActionClicked: () -> Unit,
    onSaveToDeviceClicked: () -> Unit,
    emptyViewString: String,
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scaffoldState = rememberScaffoldState()
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

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            if (state.isMultipleSelect) {
                FolderLinkSelectedTopAppBar(
                    title = title,
                    elevation = !firstItemVisible,
                    onBackPressed = onBackPressed,
                    onSelectAllClicked = onSelectAllActionClicked,
                    onClearAllClicked = onClearAllActionClicked,
                    onSaveToDeviceClicked = onSaveToDeviceClicked
                )
            } else {
                FolderLinkTopAppBar(
                    title = title,
                    elevation = !firstItemVisible,
                    onBackPressed = onBackPressed,
                    onShareClicked = onShareClicked,
                    onMoreClicked = onMoreClicked
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
            NodesView(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                nodeUIItems = state.nodesList,
                stringUtilWrapper = stringUtilWrapper,
                onMenuClick = onMenuClick,
                onItemClicked = onItemClicked,
                onLongClick = onLongClick,
                sortOrder = "",
                isListView = state.currentViewType == ViewType.LIST,
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                showSortOrder = false,
                listState = listState,
                gridState = gridState
            )
        }
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

            IconButton(onClick = onMoreClicked) {
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