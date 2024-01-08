package mega.privacy.android.app.presentation.node.label

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dividers.DividerSpacing
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun LabelRow(
    label: Label,
    onClick: (Label) -> Unit,
) {
    Column(
        modifier = Modifier
            .height(56.dp)
            .clickable { onClick(label) },
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .size(24.dp),
                painter = painterResource(id = if (label.isSelected) R.drawable.ic_label_checked else R.drawable.ic_label_unchecked),
                contentDescription = "Label icon",
                colorFilter = ColorFilter.tint(color = colorResource(id = label.labelColor))
            )
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = stringResource(id = label.labelName),
                style = MaterialTheme.typography.subtitle1,
                color = if (label.isSelected) colorResource(id = label.labelColor) else Color.Unspecified
            )
        }
        MegaDivider(
            dividerSpacing = DividerSpacing.StartBig
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LabelRowPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        LabelRow(
            Label(
                label = NodeLabel.RED,
                labelColor = R.color.red_600_red_300,
                labelName = R.string.label_red,
                isSelected = true
            )
        ) {}
    }
}