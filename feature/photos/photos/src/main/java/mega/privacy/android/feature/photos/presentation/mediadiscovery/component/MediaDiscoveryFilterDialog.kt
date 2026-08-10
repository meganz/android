package mega.privacy.android.feature.photos.presentation.mediadiscovery.component

import androidx.compose.runtime.Composable
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
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun MediaDiscoveryFilterDialog(
    selectedOrder: Int,
    onDismissRequest: () -> Unit,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filterOptions = FilterMediaType.entries
        .map {
            BasicDialogRadioOption(
                ordinal = it.ordinal,
                text = it.text()
            )
        }.toImmutableList()

    BasicRadioDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = SpannableText(stringResource(sharedR.string.general_action_filter)),
        options = filterOptions,
        selectedOption = filterOptions.find { it.ordinal == selectedOrder },
        onOptionSelected = { basicDialogRadioOption ->
            val selectedFilter = filterOptions.first {
                it.ordinal == basicDialogRadioOption.ordinal
            }
            onOptionSelected(selectedFilter.ordinal)
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
private fun FilterMediaType.text(): String = when (this) {
    FilterMediaType.ALL_MEDIA -> stringResource(sharedR.string.media_discovery_filter_all_media)
    FilterMediaType.IMAGES -> stringResource(sharedR.string.media_discovery_filter_images)
    FilterMediaType.VIDEOS -> stringResource(sharedR.string.media_discovery_filter_videos)
}

@CombinedThemePreviews
@Composable
private fun MediaDiscoveryFilterDialogPreview() {
    AndroidThemeForPreviews {
        MediaDiscoveryFilterDialog(
            selectedOrder = FilterMediaType.ALL_MEDIA.ordinal,
            onDismissRequest = {},
            onOptionSelected = {}
        )
    }
}
