package mega.privacy.android.feature.settings.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.divider.StrongDivider
import mega.android.core.ui.components.settings.SettingsOptionsItem
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.feature.settings.presentation.model.SortingAndViewModeSettingsUiState
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun SortingAndViewModeSettingsView(
    uiState: SortingAndViewModeSettingsUiState,
    onSetSortingPreference: (SortingPreference) -> Unit,
    onSetViewModePreference: (ViewModePreference) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffold(
        modifier = modifier
            .testTag(SORTING_AND_VIEW_MODE_SETTINGS_VIEW_TAG)
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.settings_sorting_and_view_mode_title),
                navigationType = AppBarNavigationType.Back(onNavigateBack),
            )
        },
    ) { paddingValues ->
        val contentModifier = Modifier.padding(paddingValues)
        when (uiState) {
            is SortingAndViewModeSettingsUiState.Data -> SettingsContent(
                modifier = contentModifier,
                sortingPreference = uiState.sortingPreference,
                viewModePreference = uiState.viewModePreference,
                onSetSortingPreference = onSetSortingPreference,
                onSetViewModePreference = onSetViewModePreference,
            )

            SortingAndViewModeSettingsUiState.Loading -> LoadingSkeleton(modifier = contentModifier)
        }
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier,
    sortingPreference: SortingPreference,
    viewModePreference: ViewModePreference,
    onSetSortingPreference: (SortingPreference) -> Unit,
    onSetViewModePreference: (ViewModePreference) -> Unit,
) {
    val perFolderLabel = stringResource(sharedR.string.settings_preference_option_per_folder)
    val allFoldersLabel = stringResource(sharedR.string.settings_preference_option_all_folders)

    Column(modifier = modifier) {
        SettingsOptionsItem(
            key = SORTING_PREFERENCE_TAG,
            title = stringResource(sharedR.string.settings_sorting_preference_title),
            values = SortingPreference.values().toList(),
            selectedValue = sortingPreference,
            valueToString = { if (it == SortingPreference.PerFolder) perFolderLabel else allFoldersLabel },
        ) { _, value ->
            onSetSortingPreference(value)
        }
        PreferenceDescription(stringResource(sharedR.string.settings_sorting_preference_description))
        StrongDivider(modifier = Modifier.fillMaxWidth())
        SettingsOptionsItem(
            key = VIEW_MODE_PREFERENCE_TAG,
            title = stringResource(sharedR.string.settings_view_mode_preference_title),
            values = ViewModePreference.values().toList(),
            selectedValue = viewModePreference,
            valueToString = { if (it == ViewModePreference.PerFolder) perFolderLabel else allFoldersLabel },
        ) { _, value ->
            onSetViewModePreference(value)
        }
        PreferenceDescription(stringResource(sharedR.string.settings_view_mode_preference_description))
        StrongDivider(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PreferenceDescription(text: String) {
    // Matches core-ui SettingsWithFooter styling (that helper is internal and
    // SettingsOptionsItem does not expose a footerText param).
    MegaText(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 5.dp, bottom = 16.dp),
        text = text,
        textColor = TextColor.Secondary,
        style = AppTheme.typography.bodyMedium,
    )
}

@Composable
private fun LoadingSkeleton(modifier: Modifier) {
    Column(modifier = modifier.testTag(SORTING_AND_VIEW_MODE_SETTINGS_SKELETON_TAG)) {
        repeat(2) {
            SkeletonPreferenceItem()
            StrongDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SkeletonPreferenceItem() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(16.dp)
                .shimmerEffect(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(12.dp)
                .shimmerEffect(),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SortingAndViewModeSettingsViewPreview() {
    AndroidThemeForPreviews {
        SortingAndViewModeSettingsView(
            uiState = SortingAndViewModeSettingsUiState.Data(
                sortingPreference = SortingPreference.PerFolder,
                viewModePreference = ViewModePreference.AllFolders,
            ),
            onSetSortingPreference = {},
            onSetViewModePreference = {},
            onNavigateBack = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SortingAndViewModeSettingsViewLoadingPreview() {
    AndroidThemeForPreviews {
        SortingAndViewModeSettingsView(
            uiState = SortingAndViewModeSettingsUiState.Loading,
            onSetSortingPreference = {},
            onSetViewModePreference = {},
            onNavigateBack = {},
        )
    }
}

internal const val SORTING_AND_VIEW_MODE_SETTINGS_VIEW_TAG = "sorting_and_view_mode_settings_view"
internal const val SORTING_AND_VIEW_MODE_SETTINGS_SKELETON_TAG =
    "$SORTING_AND_VIEW_MODE_SETTINGS_VIEW_TAG:skeleton"
internal const val SORTING_PREFERENCE_TAG =
    "$SORTING_AND_VIEW_MODE_SETTINGS_VIEW_TAG:sorting_preference"
internal const val VIEW_MODE_PREFERENCE_TAG =
    "$SORTING_AND_VIEW_MODE_SETTINGS_VIEW_TAG:view_mode_preference"
