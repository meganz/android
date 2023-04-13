package mega.privacy.android.core.ui.controls

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_012

/**
 * A custom divider
 *
 * @param withStartPadding True, if has start padding. False, if not
 */
@Composable
fun CustomDivider(
    withStartPadding: Boolean, modifier: Modifier = Modifier,
) {
    Divider(
        modifier = modifier.padding(
            start = if (withStartPadding) 72.dp else 0.dp,
            end = 0.dp
        ),
        color = grey_alpha_012.takeIf { MaterialTheme.colors.isLight } ?: white_alpha_012,
        thickness = 1.dp)
}