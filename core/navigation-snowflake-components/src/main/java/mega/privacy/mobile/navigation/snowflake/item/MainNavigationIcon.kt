package mega.privacy.mobile.navigation.snowflake.item

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

@Composable
internal fun MainNavigationIcon(
    icon: ImageVector, label: String, modifier: Modifier = Modifier,
) {
    Icon(
        modifier = modifier.size(32.dp),
        painter = rememberVectorPainter(icon),
        contentDescription = label
    )
}