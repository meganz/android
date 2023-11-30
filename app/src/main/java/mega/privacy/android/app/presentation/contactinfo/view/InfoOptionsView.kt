@file:OptIn(ExperimentalMaterialApi::class)

package mega.privacy.android.app.presentation.contactinfo.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary


@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun InfoOptionsView(
    primaryDisplayName: String,
    secondaryDisplayName: String?,
    modifyNickNameTextId: Int,
    email: String?,
    coroutineScope: CoroutineScope,
    modalSheetState: ModalBottomSheetState,
    hasAlias: Boolean,
    updateNickNameDialogVisibility: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) = Column(modifier = modifier.padding(start = 72.dp, top = 16.dp)) {
    Text(
        text = secondaryDisplayName ?: primaryDisplayName,
        style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.textColorPrimary),
    )
    email?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorPrimary),
        )
    }
    TextMegaButton(
        modifier = Modifier.padding(top = 8.dp),
        contentPadding = PaddingValues(0.dp),
        text = stringResource(id = modifyNickNameTextId),
        onClick = {
            if (hasAlias) {
                coroutineScope.launch {
                    if (modalSheetState.currentValue == ModalBottomSheetValue.Hidden) {
                        modalSheetState.show()
                    } else {
                        modalSheetState.hide()
                    }
                }
            } else {
                updateNickNameDialogVisibility(true)
            }
        },
    )
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun PreviewInfoOptionsLight() {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false,
    )
    MegaAppTheme(isDark = false) {
        Surface {
            InfoOptionsView(
                primaryDisplayName = "Nick Name",
                secondaryDisplayName = "name",
                modifyNickNameTextId = 1,
                email = "test@gmail.com",
                coroutineScope = coroutineScope,
                modalSheetState = modalSheetState,
                updateNickNameDialogVisibility = {},
                hasAlias = true
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun PreviewInfoOptionsDark() {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false,
    )
    MegaAppTheme(isDark = true) {
        Surface {
            InfoOptionsView(
                primaryDisplayName = "Nick Name",
                secondaryDisplayName = "name",
                modifyNickNameTextId = 1,
                email = "test@gmail.com",
                coroutineScope = coroutineScope,
                modalSheetState = modalSheetState,
                updateNickNameDialogVisibility = {},
                hasAlias = true
            )
        }
    }
}