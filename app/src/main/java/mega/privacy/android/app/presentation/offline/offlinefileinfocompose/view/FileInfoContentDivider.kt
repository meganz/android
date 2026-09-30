package mega.privacy.android.app.presentation.offline.offlinefileinfocompose.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_012_white_alpha_012

/**
 * Divider separating the sections of a file info surface.
 */
@Composable
internal fun FileInfoContentDivider(
    paddingStart: Dp = paddingStartDefault.dp,
    paddingTop: Dp = 0.dp,
    paddingBottom: Dp = 0.dp,
) {
    Divider(
        modifier = Modifier.padding(start = paddingStart, top = paddingTop, bottom = paddingBottom),
        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
        thickness = 1.dp
    )
}
