package mega.privacy.mobile.home.presentation.whatsnew

import androidx.compose.runtime.Composable
import mega.privacy.android.navigation.contract.NavigationHandler

interface WhatsNewDetail {
    val screen: @Composable (NavigationHandler, onHandled: () -> Unit) -> Unit
}