package mega.privacy.mobile.home.presentation.home.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

data class HomeWidgetItem(
    val identifier: String,
    val content: @Composable (modifier: Modifier, navigationHandler: NavigationHandler, transferHandler: TransferHandler) -> Unit,
)