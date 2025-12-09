package mega.privacy.android.feature.photos.presentation.videos.view

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.photos.components.FilterOptionWithRadioButton
import mega.privacy.android.feature.photos.presentation.videos.model.FilterOption

@SuppressLint("ComposeUnstableCollections")
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun VideosFilterBottomSheet(
    sheetState: SheetState,
    title: String,
    selectedFilterOption: FilterOption,
    options: List<FilterOption>,
    onItemSelected: (FilterOption) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
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
                    FilterOptionWithRadioButton(
                        modifier = Modifier.padding(vertical = 5.dp).testTag(
                            FILTER_OPTION_ITEM_TEST_TAG + option
                        ),
                        title = stringResource(option.titleResId),
                        selected = option == selectedFilterOption,
                        onClick = { onItemSelected(option) }
                    )
                }
            }
        }
    )
}

const val FILTER_OPTION_ITEM_TEST_TAG = "FILTER_OPTION_ITEM_TEST_TAG_"