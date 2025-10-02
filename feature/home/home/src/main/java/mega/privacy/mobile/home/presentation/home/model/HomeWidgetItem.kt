package mega.privacy.mobile.home.presentation.home.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey

data class HomeWidgetItem(
    val identifier: String,
    val content: @Composable (modifier: Modifier, onNavigate: (NavKey) -> Unit) -> Unit,
)