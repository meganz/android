package mega.privacy.android.feature.sync.ui.megapicker

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.legacy.core.ui.controls.appbar.LegacyTopAppBar
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.mapper.FileTypeIconMapper
import nz.mega.sdk.MegaApiJava

@Composable
internal fun MegaPickerScreen(
    currentFolder: Node?,
    nodes: List<TypedNode>,
    folderClicked: (TypedNode) -> Unit,
    currentFolderSelected: () -> Unit,
    fileTypeIconMapper: FileTypeIconMapper
) {

    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val showCurrentFolderName =
        currentFolder != null &&
                currentFolder.parentId != NodeId(MegaApiJava.INVALID_HANDLE)

    Scaffold(
        topBar = {
            LegacyTopAppBar(
                title = currentFolder?.name?.takeIf { showCurrentFolderName }
                    ?: stringResource(R.string.sync_toolbar_title),
                subtitle = if (showCurrentFolderName) {
                    null
                } else {
                    "Select folder"
                },
                elevation = false,
                onBackPressed = {
                    onBackPressedDispatcher?.onBackPressed()
                }
            )
        }, content = { paddingValues ->
            MegaPickerScreenContent(
                nodes,
                folderClicked,
                currentFolderSelected,
                Modifier.padding(paddingValues),
                fileTypeIconMapper
            )
        }
    )
}

@Composable
private fun MegaPickerScreenContent(
    nodes: List<TypedNode>,
    folderClicked: (TypedNode) -> Unit,
    currentFolderSelected: () -> Unit,
    modifier: Modifier = Modifier,
    fileTypeIconMapper: FileTypeIconMapper
) {
    Column(modifier) {
        MegaFolderPickerView(
            modifier =
            Modifier
                .padding(start = 12.dp, top = 8.dp, end = 12.dp)
                .weight(1f),
            onSortOrderClick = {},
            onChangeViewTypeClick = {},
            nodesList = nodes,
            sortOrder = "Name",
            showSortOrder = true,
            showChangeViewType = true,
            listState = LazyListState(),
            onFolderClick = {
                folderClicked(it)
            },
            fileTypeIconMapper = fileTypeIconMapper
        )

        Box(
            Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            RaisedDefaultMegaButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 8.dp),
                textId = R.string.general_select_to_download,
                onClick = {
                    currentFolderSelected()
                },
            )
        }
    }

}

@CombinedThemePreviews
@Composable
private fun PreviewSyncNewFolderScreen() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaPickerScreen(
            null,
            SampleNodeDataProvider.values,
            {},
            {},
            FileTypeIconMapper()
        )
    }
}