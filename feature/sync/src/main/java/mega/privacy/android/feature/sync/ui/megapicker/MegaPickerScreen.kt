package mega.privacy.android.feature.sync.ui.megapicker

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaSnackbar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.ui.createnewfolder.CreateNewFolderDialog
import mega.privacy.android.feature.sync.ui.createnewfolder.model.CreateNewFolderMenuAction
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaApiJava

@Composable
internal fun MegaPickerScreen(
    currentFolder: Node?,
    nodes: List<TypedNodeUiModel>?,
    folderClicked: (TypedNode) -> Unit,
    currentFolderSelected: () -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    errorMessageId: Int?,
    errorMessageShown: () -> Unit,
    isLoading: Boolean,
    isSelectEnabled: Boolean,
    onCreateNewFolderDialogSuccess: (String) -> Unit = {},
    isStopBackupMegaPicker: Boolean = false,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val showCurrentFolderName =
        currentFolder != null &&
                currentFolder.parentId != NodeId(MegaApiJava.INVALID_HANDLE)

    var showCreateNewFolderDialog by rememberSaveable { mutableStateOf(false) }

    MegaScaffold(
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = currentFolder?.name?.takeIf { showCurrentFolderName }
                    ?: stringResource(sharedR.string.general_section_cloud_drive),
                subtitle = stringResource(sharedR.string.general_select_create_folder).takeIf { isStopBackupMegaPicker },
                windowInsets = WindowInsets(0.dp),
                elevation = 0.dp,
                onNavigationPressed = {
                    onBackPressedDispatcher?.onBackPressed()
                },
                actions = mutableListOf<MenuAction>(CreateNewFolderMenuAction()),
                onActionPressed = {
                    when (it) {
                        is CreateNewFolderMenuAction -> {
                            showCreateNewFolderDialog = true
                        }
                    }
                },
            )
        }, content = { paddingValues ->
            MegaPickerScreenContent(
                nodes = nodes,
                folderClicked = folderClicked,
                currentFolderSelected = currentFolderSelected,
                fileTypeIconMapper = fileTypeIconMapper,
                modifier = Modifier.padding(paddingValues),
                isLoading = isLoading,
                isSelectEnabled = isSelectEnabled,
                isStopBackupMegaPicker = isStopBackupMegaPicker,
            )
        },
        snackbarHost = {
            MegaSnackbar(snackbarHostState)
        }
    )

    if (showCreateNewFolderDialog) {
        currentFolder?.let {
            CreateNewFolderDialog(
                currentFolder = currentFolder,
                onSuccess = { newFolderName ->
                    showCreateNewFolderDialog = false
                    onCreateNewFolderDialogSuccess(newFolderName)
                },
                onCancel = { showCreateNewFolderDialog = false },
            )
        }
    }

    LaunchedEffect(errorMessageId) {
        if (errorMessageId != null) {
            snackbarHostState.showAutoDurationSnackbar(
                message = context.resources.getString(errorMessageId),
            )
            errorMessageShown()
        }
    }
}

@Composable
private fun MegaPickerScreenContent(
    nodes: List<TypedNodeUiModel>?,
    folderClicked: (TypedNode) -> Unit,
    currentFolderSelected: () -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
    isLoading: Boolean,
    isSelectEnabled: Boolean,
    isStopBackupMegaPicker: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Main content area
        MegaFolderPickerView(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 12.dp,
                    top = 8.dp,
                    end = 12.dp,
                    bottom = if (isSelectEnabled) 80.dp else 8.dp
                ),
            onSortOrderClick = {},
            onChangeViewTypeClick = {},
            nodesList = nodes,
            sortOrder = "",
            showSortOrder = false,
            showChangeViewType = false,
            listState = LazyListState(),
            onFolderClick = {
                folderClicked(it)
            },
            fileTypeIconMapper = fileTypeIconMapper,
            isLoading = isLoading,
        )

        // Button positioned at the bottom
        if (isSelectEnabled && isLoading.not()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                RaisedDefaultMegaButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 8.dp),
                    textId = if (isStopBackupMegaPicker) {
                        sharedR.string.general_select
                    } else {
                        sharedR.string.general_select_folder
                    },
                    onClick = {
                        currentFolderSelected()
                    },
                    enabled = true,
                )
            }
        }
    }

}

@CombinedThemePreviews
@Composable
private fun SyncNewFolderScreenPreview(
    @PreviewParameter(BooleanProvider::class) isSelectEnabled: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaPickerScreen(
            null,
            SampleNodeDataProvider.values,
            {},
            {},
            FileTypeIconMapper(),
            errorMessageId = null,
            errorMessageShown = {},
            isLoading = false,
            isSelectEnabled = isSelectEnabled,
        )
    }
}
