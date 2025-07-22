@file:OptIn(ExperimentalMaterial3Api::class)

package mega.privacy.android.core.nodecomponents.sheet.nodeactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack

@Composable
fun NodeMoreOptionsBottomSheet(
    options: List<NodeActionUiOption>,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    onOptionSelected: ((NodeActionUiOption) -> Unit)? = null,
    onHelpClicked: ((NodeActionUiOption) -> Unit)? = null,
) {
    MegaModalBottomSheet(
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        sheetState = sheetState,
        modifier = modifier,
        onDismissRequest = onDismissRequest
    ) {
        options.distinct().forEach { option ->
            val label = remember(option) { option.label }

            FlexibleLineListItem(
                modifier = Modifier
                    .testTag(option.key),
                title = label.text,
                titleTextColor = option.textColor,
                leadingElement = {
                    NodeActionIcon(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp),
                        option = option,
                        contentDescription = label.text
                    )
                },
                trailingElement = {
                    if (option.showHelpButton) {
                        HelpIcon(
                            option = option,
                            contentDescription = "Help for ${label.text}",
                            onHelpClicked = onHelpClicked
                        )
                    }
                },
                onClickListener = {
                    onOptionSelected?.invoke(option)
                }
            )
        }
    }
}

@Composable
private fun NodeActionIcon(
    modifier: Modifier,
    option: NodeActionUiOption,
    contentDescription: String,
) {
    if (option.textColor == TextColor.Error) {
        MegaIcon(
            modifier = modifier
                .testTag(ERROR_NODE_ICON_TAG)
                .size(24.dp),
            painter = rememberVectorPainter(option.icon),
            contentDescription = contentDescription,
            textColorTint = option.textColor,
        )
    } else {
        MegaIcon(
            modifier = modifier
                .testTag(NODE_ICON_TAG)
                .size(24.dp),
            painter = rememberVectorPainter(option.icon),
            contentDescription = contentDescription,
            tint = IconColor.Secondary,
        )
    }
}

@Composable
private fun HelpIcon(
    option: NodeActionUiOption,
    contentDescription: String,
    onHelpClicked: ((NodeActionUiOption) -> Unit)?,
) {
    MegaIcon(
        modifier = Modifier
            .size(24.dp)
            .testTag(HELP_ICON_TAG)
            .clickable { onHelpClicked?.invoke(option) },
        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.HelpCircle),
        contentDescription = contentDescription,
        tint = IconColor.Secondary,
    )
}

@Composable
@CombinedThemePreviews
private fun NodeMoreOptionsBottomSheetPreview() {
    AndroidThemeForPreviews {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        LaunchedEffect(Unit) {
            sheetState.show()
        }

        NodeMoreOptionsBottomSheet(
            options = NodeActionUiOption.defaults,
            sheetState = sheetState,
        )
    }
}

internal const val HELP_ICON_TAG = "NodeMoreOptionsBottomSheet:help_icon"
internal const val ERROR_NODE_ICON_TAG = "NodeMoreOptionsBottomSheet:error_node_icon"
internal const val NODE_ICON_TAG = "NodeMoreOptionsBottomSheet:node_icon"