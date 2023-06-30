package mega.privacy.android.feature.sync.ui.megapicker

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.R

@Composable
internal fun MegaPickerScreen(
    nodes: List<TypedNode>,
    folderClicked: (TypedNode) -> Unit,
    currentFolderSelected: () -> Unit,
) {

    Column {
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
            getThumbnail = { _, _ -> },
            onFolderClick = {
                folderClicked(it)
            },
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
            SampleNodeDataProvider.values,
            {},
            {}
        )
    }
}