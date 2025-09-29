package mega.privacy.android.navigation.contract.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class HomeWidgetViewHolder(
    val widgetFunction: @Composable (Modifier) -> Unit,
)
