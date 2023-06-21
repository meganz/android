package mega.privacy.android.app.presentation.chat.list.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012

/**
 * Chat item divider
 *
 * @param modifier
 * @param startPadding
 */
@Composable
fun ChatDivider(
    modifier: Modifier = Modifier,
    startPadding: Dp = 72.dp,
) {
    Divider(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startPadding),
        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
        thickness = 1.dp
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewChatDivider() {
    ChatDivider()
}
