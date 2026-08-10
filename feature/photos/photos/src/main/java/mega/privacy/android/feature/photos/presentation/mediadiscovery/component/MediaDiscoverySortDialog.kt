package mega.privacy.android.feature.photos.presentation.mediadiscovery.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.BasicDialogRadioOption
import mega.android.core.ui.components.dialogs.BasicRadioDialog
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun MediaDiscoverySortDialog(
    selectedOrder: Int,
    onDismissRequest: () -> Unit,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortOptions = listOf(Sort.NEWEST, Sort.OLDEST)
        .map {
            BasicDialogRadioOption(
                ordinal = it.ordinal,
                text = it.text()
            )
        }.toImmutableList()

    BasicRadioDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = SpannableText(stringResource(sharedR.string.action_sort_by_header)),
        options = sortOptions,
        selectedOption = sortOptions.find { it.ordinal == selectedOrder },
        onOptionSelected = { basicDialogRadioOption ->
            val selectedSort = sortOptions.first {
                it.ordinal == basicDialogRadioOption.ordinal
            }
            onOptionSelected(selectedSort.ordinal)
        },
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                onClick = onDismissRequest
            )
        )
    )
}

@Composable
private fun Sort.text() = when (this) {
    Sort.NEWEST -> stringResource(sharedR.string.timeline_tab_sort_by_date_newest)
    Sort.OLDEST -> stringResource(sharedR.string.timeline_tab_sort_by_date_oldest)
    else -> ""
}

@CombinedThemePreviews
@Composable
private fun MediaDiscoverySortDialogPreview() {
    AndroidThemeForPreviews {
        MediaDiscoverySortDialog(
            selectedOrder = Sort.NEWEST.ordinal,
            onDismissRequest = {},
            onOptionSelected = {}
        )
    }
}
