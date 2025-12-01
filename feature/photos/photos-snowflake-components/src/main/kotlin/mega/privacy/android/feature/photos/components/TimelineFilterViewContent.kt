package mega.privacy.android.feature.photos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.android.core.ui.tokens.theme.DSTokens

@Composable
fun TimelineFilterViewContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(DSTokens.colors.background.pageBackground)
            .safeDrawingPadding()
    ) {
        content()
    }
}
