package mega.privacy.android.core.nodecomponents.sheet.changelabel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.list.NodeLabelCircle
import mega.privacy.android.core.nodecomponents.model.label.ChangeLabelState
import mega.privacy.android.core.nodecomponents.model.label.Label
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedResR

@Composable
internal fun ChangeLabelBottomSheetContentM3(
    nodeId: NodeId,
    viewModel: ChangeLabelBottomSheetViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.loadLabelInfo(nodeId)
    }
    ChangeLabelBottomSheetContentM3(
        state = state,
        onLabelSelected = { label ->
            viewModel.onLabelSelected(label)
            onDismiss()
        },
        onLabelRemoved = {
            viewModel.onLabelSelected(null)
            onDismiss()
        },
    )
}

@Composable
private fun ChangeLabelBottomSheetContentM3(
    state: ChangeLabelState,
    onLabelSelected: (NodeLabel) -> Unit,
    onLabelRemoved: () -> Unit,
) {
    Column {
        MegaText(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            text = stringResource(id = R.string.title_label),
            textColor = TextColor.Secondary,
            style = AppTheme.typography.titleMedium
        )

        LazyColumn {
            items(state.labelList.size) {
                val label = state.labelList[it]

                FlexibleLineListItem(
                    modifier = Modifier
                        .fillMaxWidth(),
                    title = stringResource(label.labelName),
                    leadingElement = {
                        NodeLabelCircle(
                            modifier = Modifier
                                .align(Alignment.Center),
                            label = label.label
                        )
                    },
                    trailingElement = {
                        if (label.isSelected) {
                            MegaIcon(
                                modifier = Modifier
                                    .size(24.dp),
                                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Check),
                                contentDescription = "Selected label icon",
                                tint = IconColor.Secondary
                            )
                        }
                    },
                    onClickListener = { onLabelSelected(label.label) }
                )
            }
            item {
                FlexibleLineListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(id = R.string.action_remove_label),
                    leadingElement = {
                        MegaIcon(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(24.dp),
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                            textColorTint = TextColor.Error
                        )
                    },
                    onClickListener = onLabelRemoved,
                    titleTextColor = TextColor.Error
                )
            }
        }
    }
}


@CombinedThemePreviews
@Composable
private fun ChangeLabelBottomSheetPreview() {
    val state = ChangeLabelState(
        labelList = listOf(
            Label(
                label = NodeLabel.RED,
                labelName = sharedResR.string.label_red,
                labelColor = R.color.label_red,
                isSelected = false
            ),
            Label(
                label = NodeLabel.ORANGE,
                labelName = sharedResR.string.label_orange,
                labelColor = R.color.label_orange,
                isSelected = true
            ),
            Label(
                label = NodeLabel.YELLOW,
                labelName = sharedResR.string.label_yellow,
                labelColor = R.color.label_yellow,
                isSelected = false
            ),
            Label(
                label = NodeLabel.GREEN,
                labelName = sharedResR.string.label_green,
                labelColor = R.color.label_green,
                isSelected = false
            ),
            Label(
                label = NodeLabel.BLUE,
                labelName = sharedResR.string.label_blue,
                labelColor = R.color.label_blue,
                isSelected = false
            ),
            Label(
                label = NodeLabel.PURPLE,
                labelName = sharedResR.string.label_purple,
                labelColor = R.color.label_purple,
                isSelected = false
            ),
        )
    )

    AndroidThemeForPreviews {
        ChangeLabelBottomSheetContentM3(
            state = state,
            onLabelSelected = {},
            onLabelRemoved = {},
        )
    }
}