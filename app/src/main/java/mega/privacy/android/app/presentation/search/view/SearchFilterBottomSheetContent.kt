package mega.privacy.android.app.presentation.search.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.presentation.search.LegacySearchViewModel
import mega.privacy.android.app.presentation.search.navigation.DATE_ADDED
import mega.privacy.android.app.presentation.search.navigation.DATE_MODIFIED
import mega.privacy.android.app.presentation.search.navigation.TYPE
import mega.privacy.android.shared.resources.R

/**
 * Search filter bottom sheet
 */
@Composable
internal fun SearchFilterBottomSheetContent(
    filter: String,
    onDismiss: () -> Unit,
    viewModel: LegacySearchViewModel,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        keyboardController?.hide()
    }

    BottomSheetContentLayout(
        modifier = Modifier,
        title = when (filter) {
            TYPE -> stringResource(id = R.string.search_dropdown_chip_filter_type_file_type)
            DATE_MODIFIED -> stringResource(id = R.string.search_dropdown_chip_filter_type_last_modified)
            DATE_ADDED -> stringResource(id = R.string.search_dropdown_chip_filter_type_date_added)
            else -> "Unknown"
        },
        options = viewModel.getFilterOptions(filter),
        onItemSelected = { item ->
            viewModel.updateFilterEntity(filter, item)
            onDismiss()
        }
    )
}
