package mega.privacy.android.navigation.contract.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey

data class HomeWidgetViewHolder(
    val widgetFunction: @Composable (Modifier, onNavigate: (NavKey) -> Unit) -> Unit,
)
