package mega.privacy.android.app.presentation.settings.startscreen.view

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.images.MegaIcon
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeComponentPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor

/**
 * Start screen option view
 *
 * @param icon
 * @param text
 * @param isSelected
 * @param onClick
 * @param modifier
 */
@Composable
fun StartScreenOptionView(
    @DrawableRes icon: Int,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .toggleable(
                value = isSelected,
                role = Role.RadioButton,
                onValueChange = { onClick() }
            )
            .height(56.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier
                .weight(1f, true)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            MegaIcon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 18.dp)
                    .size(24.dp)
                    .testTag(icon.toString()),
                tint = IconColor.Secondary,
            )
            MegaText(
                modifier = Modifier
                    .padding(start = 30.dp)
                    .weight(1f, true),
                text = text,
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Medium),
                textColor = TextColor.Primary
            )
            if (isSelected) {
                MegaIcon(
                    painter = painterResource(id = iconPackR.drawable.ic_check_medium_regular_outline),
                    tint = IconColor.Secondary,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .size(24.dp)
                        .testTag(iconPackR.drawable.ic_check_medium_regular_outline.toString())
                )
            }
        }
        MegaDivider(dividerType = DividerType.BigStartPadding)
    }
}

/**
 * Start screen option view preview
 */
@CombinedThemeComponentPreviews
@Composable
fun StartScreenOptionViewPreview(
    @PreviewParameter(BooleanProvider::class) isSelected: Boolean,
) {
    var selected by remember { (mutableStateOf(isSelected)) }
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        Box(modifier = Modifier.background(MaterialTheme.colors.surface)) {
            StartScreenOptionView(
                iconPackR.drawable.ic_mega_medium_regular_outline,
                "Home",
                isSelected = selected,
                onClick = { selected = !selected },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}