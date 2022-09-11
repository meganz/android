package mega.privacy.android.app.presentation.settings.startscreen.view

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.presentation.theme.grey_alpha_012
import mega.privacy.android.presentation.theme.grey_alpha_054
import mega.privacy.android.presentation.theme.grey_alpha_087
import mega.privacy.android.presentation.theme.white_alpha_012
import mega.privacy.android.presentation.theme.white_alpha_054
import mega.privacy.android.presentation.theme.white_alpha_087

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
    val startPadding = 18.dp
    val iconSize = 24.dp
    val textStartPadding = 30.dp
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
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 18.dp)
                    .size(24.dp)
                    .testTag(icon.toString()),
                tint = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054
            )
            Text(
                modifier = Modifier
                    .padding(start = textStartPadding)
                    .weight(1f, true),
                text = text,
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Medium),
                color = if (MaterialTheme.colors.isLight) grey_alpha_087 else white_alpha_087
            )
            if (isSelected) {
                Image(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .size(24.dp)
                        .testTag(R.drawable.ic_check.toString())
                )
            }
        }
        Divider(
            color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
            thickness = 1.dp,
            startIndent = startPadding + iconSize + textStartPadding
        )
    }
}

/**
 * Start screen option view preview
 */
@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun StartScreenOptionViewPreview() {
    var selected by remember { (mutableStateOf(true)) }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        Box(modifier = Modifier.background(MaterialTheme.colors.surface)) {
            StartScreenOptionView(
                R.drawable.ic_homepage,
                "Home",
                isSelected = selected,
                onClick = { selected = !selected },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}