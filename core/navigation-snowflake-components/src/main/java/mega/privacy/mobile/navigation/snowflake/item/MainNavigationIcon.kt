package mega.privacy.mobile.navigation.snowflake.item

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
internal fun MainNavigationIcon(iconRes: Int, label: String) {
    Icon(
        modifier = Modifier.Companion.size(32.dp),
        painter = painterResource(iconRes),
        contentDescription = label
    )
}