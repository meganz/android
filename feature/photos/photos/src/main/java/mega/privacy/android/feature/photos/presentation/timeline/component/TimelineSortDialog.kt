package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.BasicDialogRadioOption
import mega.android.core.ui.components.dialogs.BasicRadioDialog
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun TimelineSortDialog(
    selected: TimelineTabSortOptions,
    onDismissRequest: () -> Unit,
    onOptionSelected: (value: TimelineTabSortOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val selectedOption by remember(selected) {
        mutableStateOf(
            BasicDialogRadioOption(
                ordinal = selected.ordinal,
                text = context.getString(selected.nameResId)
            )
        )
    }

    BasicRadioDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = SpannableText(stringResource(sharedR.string.timeline_tab_sort_by_text)),
        options = TimelineTabSortOptions.entries.map {
            BasicDialogRadioOption(
                ordinal = it.ordinal,
                text = context.getString(it.nameResId)
            )
        }.toImmutableList(),
        selectedOption = selectedOption,
        onOptionSelected = { basicDialogRadioOption ->
            onOptionSelected(
                TimelineTabSortOptions.entries.first {
                    it.ordinal == basicDialogRadioOption.ordinal
                }
            )
        },
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                onClick = onDismissRequest
            )
        )
    )
}

@CombinedThemePreviews
@Composable
private fun TimelineSortDialogPreview() {
    AndroidThemeForPreviews {
        TimelineSortDialog(
            selected = TimelineTabSortOptions.Newest,
            onDismissRequest = {},
            onOptionSelected = {}
        )
    }
}
