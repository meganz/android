package mega.privacy.android.app.presentation.meeting.view

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary

/**
 * View of a menu item in the bottom panel
 *
 * @param res              Drawable resource.
 * @param text             String resource.
 * @param description      Text of description.
 * @param tintRed          True, tint red. False, otherwise.
 * @param onClick          Detect when menu item is clicked
 */
@Composable
fun BottomSheetMenuItemView(
    modifier: Modifier,
    @DrawableRes res: Int,
    @StringRes text: Int,
    description: String,
    tintRed: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .clickable(onClick = onClick)
    ) {
        val iconColor: Color
        val textColor: Color
        if (tintRed) {
            iconColor = MaterialTheme.colors.red_600_red_300
            textColor = MaterialTheme.colors.red_600_red_300
        } else {
            iconColor = MaterialTheme.colors.grey_alpha_054_white_alpha_054
            textColor = MaterialTheme.colors.textColorPrimary
        }
        Icon(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(24.dp)
                .align(Alignment.CenterVertically),
            painter = painterResource(id = res),
            contentDescription = description,
            tint = iconColor
        )
        Text(
            modifier = Modifier
                .padding(start = 32.dp, end = 16.dp)
                .align(Alignment.CenterVertically),
            text = stringResource(id = text),
            color = textColor,
            style = MaterialTheme.typography.subtitle1
        )
    }
}

/**
 * [BottomSheetMenuItemView] preview
 */
@Preview
@Composable
fun PreviewBottomSheetMenuItemView() {
    MegaAppTheme(isDark = true) {
        BottomSheetMenuItemView(
            modifier = Modifier,
            res = R.drawable.ic_scheduled_meeting_edit,
            text = R.string.title_edit_profile_info,
            description = "Edit",
            tintRed = true,
            onClick = {},
        )
    }
}