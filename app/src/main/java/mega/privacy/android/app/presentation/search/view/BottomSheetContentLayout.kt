package mega.privacy.android.app.presentation.search.view

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.search.model.FilterOptionEntity
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun BottomSheetContentLayout(
    title: String,
    options: List<FilterOptionEntity>,
    onItemSelected: (FilterOptionEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        MegaText(
            modifier = modifier.padding(16.dp),
            text = title,
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.subtitle1,
        )
        LazyColumn {
            items(options) { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(
                            onClick = { onItemSelected(option) })
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MegaText(
                        text = stringResource(id = option.title),
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.subtitle2,
                    )

                    if (option.isSelected) {
                        Icon(
                            painter = painterResource(id = iconPackR.drawable.ic_check_medium_regular_outline),
                            tint = colorResource(id = R.color.teal_300),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun SearchFilterBottomSheetPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        BottomSheetContentLayout(
            title = "Type",
            options = TypeFilterOption.entries.map { option ->
                FilterOptionEntity(
                    id = option.ordinal,
                    title = R.string.search_dropdown_chip_filter_type_file_type,
                    isSelected = true
                )
            },
            onItemSelected = {}
        )
    }
}
