package mega.privacy.android.app.presentation.search.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.search.SearchActivityViewModel
import mega.privacy.android.app.presentation.search.model.DateFilterOption
import mega.privacy.android.app.presentation.search.model.FilterOptionEntity
import mega.privacy.android.app.presentation.search.model.TypeFilterOption
import mega.privacy.android.app.presentation.search.navigation.DATE_ADDED
import mega.privacy.android.app.presentation.search.navigation.DATE_MODIFIED
import mega.privacy.android.app.presentation.search.navigation.TYPE

/**
 * Search filter bottom sheet
 */
@Composable
internal fun SearchFilterBottomSheetContent(
    filter: String,
    onDismiss: () -> Unit,
    viewModel: SearchActivityViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        keyboardController?.hide()
    }

    BottomSheetContentLayout(
        modifier = Modifier,
        title = when (filter) {
            TYPE -> "Type"
            DATE_MODIFIED -> "Last modified"
            DATE_ADDED -> "Date added"
            else -> "Unknown"
        },
        options = when (filter) {
            TYPE ->
                TypeFilterOption.entries.map { option ->
                    FilterOptionEntity(
                        option.ordinal,
                        option.title,
                        option == state.typeSelectedFilterOption
                    )
                }

            DATE_MODIFIED ->
                DateFilterOption.entries.map { option ->
                    FilterOptionEntity(
                        option.ordinal,
                        option.title,
                        option == state.dateModifiedSelectedFilterOption
                    )
                }

            DATE_ADDED -> DateFilterOption.entries.map { option ->
                FilterOptionEntity(
                    option.ordinal,
                    option.title,
                    option == state.dateAddedSelectedFilterOption
                )
            }

            else -> emptyList()
        },
        onItemSelected = { item ->
            when (filter) {
                TYPE -> {
                    val typeOption = TypeFilterOption.entries.getOrNull(item.id)
                        ?.takeIf { it.ordinal != state.typeSelectedFilterOption?.ordinal }
                    viewModel.setTypeSelectedFilterOption(typeOption)
                }

                DATE_MODIFIED -> {
                    val dateModifiedOption = DateFilterOption.entries.getOrNull(item.id)
                        ?.takeIf { it.ordinal != state.dateModifiedSelectedFilterOption?.ordinal }

                    viewModel.setDateModifiedSelectedFilterOption(dateModifiedOption)
                }

                DATE_ADDED -> {
                    val dateAddedOption = DateFilterOption.entries.getOrNull(item.id)
                        ?.takeIf { it.ordinal != state.dateAddedSelectedFilterOption?.ordinal }

                    viewModel.setDateAddedSelectedFilterOption(dateAddedOption)
                }
            }
            onDismiss()
        }
    )
}
