package mega.privacy.android.app.presentation.node.label

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.theme.MegaAppTheme


@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun LabelChangeBottomSheet(
    modalSheetState: ModalBottomSheetState,
    node: TypedNode,
    viewModel: ChangeLabelBottomSheetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadLabelInfo(node)
    }
    ChangeLabelBottomSheet(
        modalSheetState = modalSheetState,
        state = state,
        onLabelSelected = {
            viewModel.onLabelSelected(node.id, it)
        },
        onLabelRemoved = {
            viewModel.onLabelSelected(node.id, null)
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ChangeLabelBottomSheet(
    modalSheetState: ModalBottomSheetState,
    state: ChangeLabelState,
    onLabelSelected: (NodeLabel) -> Unit,
    onLabelRemoved: () -> Unit,
) {
    BottomSheet(
        modalSheetState = modalSheetState,
        sheetHeader = {
            MegaText(
                modifier = Modifier
                    .padding(16.dp),
                text = stringResource(id = R.string.title_label),
                textColor = TextColor.Secondary,
            )
        },
        sheetBody = {
            LazyColumn {
                items(state.labelList.size) {
                    val label = state.labelList[it]
                    LabelRow(
                        label = label,
                        onClick = { lbl ->
                            onLabelSelected(lbl.label)
                        }
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .clickable {
                                onLabelRemoved()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier.padding(horizontal = 24.dp)
                                .size(24.dp),
                            painter = painterResource(id = R.drawable.ic_close_white),
                            contentDescription = "Close icon",
                            colorFilter = ColorFilter.tint(color = colorResource(id = R.color.red_600_red_300))
                        )
                        MegaText(
                            text = stringResource(id = R.string.action_remove_label),
                            textColor = TextColor.Error,
                            style = MaterialTheme.typography.subtitle1,
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun ChangeLabelBottomSheetPreview() {
    val state = ChangeLabelState(
        labelList = listOf(
            Label(
                label = NodeLabel.RED,
                labelName = R.string.label_red,
                labelColor = R.color.red_600_red_300,
                isSelected = false
            ),
            Label(
                label = NodeLabel.GREY,
                labelColor = R.color.grey_300,
                labelName = R.string.label_grey,
                isSelected = false
            ),
            Label(
                label = NodeLabel.PURPLE,
                labelColor = R.color.purple_300_purple_200,
                labelName = R.string.label_purple,
                isSelected = true
            ),
            Label(
                label = NodeLabel.GREEN,
                labelColor = R.color.green_400_green_300,
                labelName = R.string.label_green,
                isSelected = false
            )
        )
    )
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChangeLabelBottomSheet(
            state = state,
            modalSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.HalfExpanded),
            onLabelRemoved = {},
            onLabelSelected = {}
        )
    }
}