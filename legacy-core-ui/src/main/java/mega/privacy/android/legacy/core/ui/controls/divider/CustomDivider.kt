package mega.privacy.android.legacy.core.ui.controls.divider

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012

/**
 * A custom divider
 *
 * @param withStartPadding  True, if has start padding. False, if not
 * @param startPadding      Start padding
 */
@Composable
fun CustomDivider(
    withStartPadding: Boolean, modifier: Modifier = Modifier,
    startPadding: Dp = 72.dp,
) {
    Divider(
        modifier = modifier.padding(
            start = if (withStartPadding) startPadding else 0.dp,
            end = 0.dp
        ),
        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
        thickness = 1.dp
    )
}