package mega.privacy.android.feature.photos.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.DSTokens

@Composable
fun TimelineGridSizeSettingsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        modifier = modifier,
        expanded = expanded,
        containerColor = DSTokens.colors.background.surface1,
        shape = RoundedCornerShape(4.dp),
        onDismissRequest = onDismissRequest,
        content = content
    )
}
