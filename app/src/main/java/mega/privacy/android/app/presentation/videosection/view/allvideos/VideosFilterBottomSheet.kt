package mega.privacy.android.app.presentation.videosection.view.allvideos

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideosFilterOptionEntity
import mega.privacy.android.shared.original.core.ui.controls.lists.SettingsItemWithRadioButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun VideosFilterBottomSheet(
    modifier: Modifier,
    sheetState: SheetState,
    title: String,
    options: List<VideosFilterOptionEntity>,
    onItemSelected: (VideosFilterOptionEntity) -> Unit,
    onDismissRequest: () -> Unit,
) {
    MegaModalBottomSheet(
        modifier = modifier,
        bottomSheetBackground = MegaModalBottomSheetBackground.PageBackground,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                MegaText(
                    modifier = Modifier.padding(16.dp),
                    text = title,
                    textColor = TextColor.Primary
                )

                options.map { option ->
                    SettingsItemWithRadioButton(
                        modifier = Modifier.padding(vertical = 5.dp),
                        title = option.title,
                        selected = option.isSelected,
                        onClick = { onItemSelected(option) }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun VideosLocationFilterBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        VideosFilterBottomSheet(
            modifier = Modifier,
            title = "Location",
            sheetState = rememberModalBottomSheetState { true },
            options = LocationFilterOption.entries.map { option ->
                VideosFilterOptionEntity(
                    id = option.ordinal,
                    title = stringResource(id = option.titleResId),
                    isSelected = false
                )
            },
            onItemSelected = {},
            onDismissRequest = {}
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun VideosDurationFilterBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        VideosFilterBottomSheet(
            modifier = Modifier,
            title = "Duration",
            sheetState = rememberModalBottomSheetState { true },
            options = DurationFilterOption.entries.map { option ->
                VideosFilterOptionEntity(
                    id = option.ordinal,
                    title = stringResource(id = option.titleResId),
                    isSelected = option == DurationFilterOption.MoreThan20
                )
            },
            onItemSelected = {},
            onDismissRequest = {}
        )
    }
}