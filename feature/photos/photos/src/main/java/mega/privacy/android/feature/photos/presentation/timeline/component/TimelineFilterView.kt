package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.button.MegaRadioButton
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.chip.DefaultChipStyle
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.SecondaryHeaderListItem
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.photos.components.TimelineFilterViewContent
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun TimelineFilterView(
    currentFilter: TimelineFilterUiState,
    onApplyFilterClick: (request: TimelineFilterRequest) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedMediaType by rememberSaveable(currentFilter.mediaType) {
        mutableStateOf(currentFilter.mediaType)
    }
    var selectedMediaSource by rememberSaveable(currentFilter.mediaSource) {
        mutableStateOf(currentFilter.mediaSource)
    }
    var isRemembered by rememberSaveable(currentFilter.isRemembered) {
        mutableStateOf(currentFilter.isRemembered)
    }

    TimelineFilterViewContent(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1F)
                .verticalScroll(rememberScrollState())
        ) {
            MegaTopAppBar(
                title = stringResource(sharedR.string.timeline_tab_filter_text),
                navigationType = AppBarNavigationType.Close(onNavigationIconClicked = onClose),
            )

            MediaTypeSectionBody(
                modifier = Modifier.fillMaxWidth(),
                selected = selectedMediaType,
                onSelected = { selectedMediaType = it }
            )

            MediaSourceSectionBody(
                modifier = Modifier.fillMaxWidth(),
                selected = selectedMediaSource,
                onSelected = { selectedMediaSource = it }
            )

            FlexibleLineListItem(
                modifier = Modifier.fillMaxWidth(),
                minHeight = 58.dp,
                contentPadding = PaddingValues(horizontal = 16.dp),
                title = stringResource(sharedR.string.timeline_tab_filter_remember_preferences),
                trailingElement = {
                    Toggle(
                        isChecked = isRemembered,
                        onCheckedChange = { isRemembered = it }
                    )
                },
                onClickListener = { isRemembered = !isRemembered },
            )
        }

        PrimaryFilledButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag(TIMELINE_FILTER_VIEW_APPLY_FILTER_BUTTON_TAG),
            text = stringResource(sharedR.string.timeline_tab_filter_apply_filter_button),
            onClick = {
                onApplyFilterClick(
                    TimelineFilterRequest(
                        isRemembered = isRemembered,
                        mediaType = selectedMediaType,
                        mediaSource = selectedMediaSource
                    )
                )
            },
        )
    }
}

@Composable
private fun MediaTypeSectionBody(
    selected: FilterMediaType,
    onSelected: (value: FilterMediaType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SecondaryHeaderListItem(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = sharedR.string.timeline_tab_filter_media_type_selection_header)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterMediaType.entries.forEach {
                val isSelected = it == selected
                MegaChip(
                    selected = isSelected,
                    content = when (it) {
                        FilterMediaType.ALL_MEDIA -> stringResource(id = sharedR.string.timeline_tab_filter_media_type_all_media)
                        FilterMediaType.IMAGES -> stringResource(id = sharedR.string.search_dropdown_chip_filter_type_file_type_images)
                        FilterMediaType.VIDEOS -> stringResource(id = sharedR.string.media_videos_tab_title)
                    },
                    style = DefaultChipStyle,
                    leadingPainter = rememberVectorPainter(image = IconPack.Small.Thin.Outline.Check).takeIf { isSelected },
                    onClick = { onSelected(it) }
                )
            }
        }
    }
}

@Composable
private fun MediaSourceSectionBody(
    selected: FilterMediaSource,
    onSelected: (value: FilterMediaSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SecondaryHeaderListItem(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = sharedR.string.timeline_tab_filter_media_source_selection_header)
        )

        FilterMediaSource.entries.forEach { mediaSource ->
            FlexibleLineListItem(
                modifier = Modifier.fillMaxWidth(),
                minHeight = 58.dp,
                contentPadding = PaddingValues(start = 16.dp),
                title = stringResource(mediaSource.nameResId),
                trailingElement = {
                    MegaRadioButton(
                        identifier = mediaSource,
                        selected = mediaSource == selected,
                        onOptionSelected = { onSelected(it as FilterMediaSource) }
                    )
                },
                onClickListener = { onSelected(mediaSource) },
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun TimelineFilterBodyPreview() {
    AndroidThemeForPreviews {
        TimelineFilterView(
            modifier = Modifier.fillMaxSize(),
            currentFilter = TimelineFilterUiState(),
            onApplyFilterClick = {},
            onClose = {}
        )
    }
}

internal const val TIMELINE_FILTER_VIEW_APPLY_FILTER_BUTTON_TAG =
    "timeline_filter_view:button_apply_filter"
