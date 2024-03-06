package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.search.model.TypeFilterOption
import mega.privacy.android.app.presentation.search.model.TypeFilterOptionEntity
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.shared.theme.MegaAppTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun TypeFilterBottomSheet(
    modifier: Modifier,
    modalSheetState: ModalBottomSheetState,
    title: String,
    options: List<TypeFilterOptionEntity>,
    onItemSelected: (TypeFilterOptionEntity) -> Unit,
) {
    BottomSheet(modalSheetState = modalSheetState,
        sheetBody = {
            Column {
                MegaText(
                    modifier = modifier.padding(16.dp),
                    text = title,
                    textColor = TextColor.Secondary,
                    style = MaterialTheme.typography.subtitle1,
                )

                options.map { option ->
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
                            text = option.title,
                            textColor = TextColor.Primary,
                            style = MaterialTheme.typography.subtitle2,
                        )

                        if (option.isSelected) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun TypeFilterBottomSheetPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        TypeFilterBottomSheet(
            modifier = Modifier,
            modalSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Expanded,
                skipHalfExpanded = false,
            ),
            title = "Type",
            options = TypeFilterOption.entries.map { option ->
                TypeFilterOptionEntity(
                    id = option.ordinal,
                    title = option.title,
                    isSelected = true
                )
            },
            onItemSelected = {}
        )
    }
}
