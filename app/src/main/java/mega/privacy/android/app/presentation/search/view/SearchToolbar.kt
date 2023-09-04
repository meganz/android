package mega.privacy.android.app.presentation.search.view

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.appbar.ExpandedSearchAppBar
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

/**
 * Search toolbar used in search activity
 */
@Composable
fun SearchToolBar(selectionMode: Boolean, selectionCount: Int) {
    var searchQuery by remember { mutableStateOf("") }
    if (selectionMode) {
        TopAppBar(
            title = {
                Text(text = "$selectionCount")
            },
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back button",
                        tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White
                    )
                }
            },
            backgroundColor = MaterialTheme.colors.surface,
            actions = {
                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download_white),
                        contentDescription = "download icon"
                    )
                }
            },
        )
    } else {
        ExpandedSearchAppBar(
            text = searchQuery,
            hintId = R.string.hint_action_search,
            onSearchTextChange = { searchQuery = it },
            onCloseClicked = { searchQuery = "" },
            elevation = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewSearchToolbar(
    @PreviewParameter(BooleanProvider::class) selectionMode: Boolean,
) {
    SearchToolBar(selectionMode = selectionMode, selectionCount = 10)
}