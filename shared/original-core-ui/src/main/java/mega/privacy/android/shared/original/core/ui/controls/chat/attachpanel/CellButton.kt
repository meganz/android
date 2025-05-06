package mega.privacy.android.shared.original.core.ui.controls.chat.attachpanel

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalThemeForPreviews
import mega.privacy.android.shared.original.core.ui.theme.extensions.body4

/**
 * Cell button usually used in bottom sheets or toolbars
 *
 * @param iconId Icon resource id
 * @param itemName Item name
 * @param onItemClick Item click action
 * @param modifier [Modifier]
 */
@Composable
fun CellButton(
    @DrawableRes iconId: Int,
    itemName: String,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: CellButtonType = CellButtonType.On,
    enabled: Boolean = true,
) = Column(
    modifier = modifier
        .size(width = 64.dp, height = 66.dp)
        .clickable(enabled = enabled, onClick = onItemClick),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else .3f)
            .background(
                color = when (type) {
                    CellButtonType.On -> DSTokens.colors.button.secondary
                    CellButtonType.Off -> DSTokens.colors.background.inverse
                    CellButtonType.Interactive -> DSTokens.colors.components.interactive
                },
                shape = CircleShape
            )
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = itemName,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center)
                .testTag(TEST_TAG_CELL_BUTTON_ICON),
            tint = when (type) {
                CellButtonType.On, CellButtonType.Interactive -> DSTokens.colors.icon.primary
                CellButtonType.Off -> DSTokens.colors.icon.inverse
            }
        )
    }
    Text(
        text = itemName,
        style = MaterialTheme.typography.body4,
        modifier = Modifier.padding(top = 2.dp),
        color = DSTokens.colors.text.primary
    )
}

/**
 * Type for [CellButton]
 */
enum class CellButtonType {
    On, Off, Interactive,
}

@Composable
fun CellButtonPlaceHolder(modifier: Modifier = Modifier) =
    Box(modifier = modifier.size(width = 64.dp, height = 66.dp))

@CombinedThemePreviews
@Composable
private fun CellButtonPreview(
    @PreviewParameter(CellButtonTypeProvider::class) typeAndEnabled: Pair<CellButtonType, Boolean>,
) {
    OriginalThemeForPreviews {
        CellButton(
            iconId = R.drawable.ic_menu,
            itemName = "Item",
            onItemClick = {},
            type = typeAndEnabled.first,
            enabled = typeAndEnabled.second,
        )
    }
}

private class CellButtonTypeProvider : PreviewParameterProvider<Pair<CellButtonType, Boolean>> {
    override val values = CellButtonType.entries
        .flatMap { listOf(it to true, it to false) }.asSequence()
}

internal const val TEST_TAG_CELL_BUTTON_ICON = "chat_view:attach_panel:attach_icon"