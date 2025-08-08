package mega.privacy.android.core.nodecomponents.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.megaNavigator

@Composable
fun rememberMegaNavigator(): MegaNavigator {
    val context = LocalContext.current
    return remember { context.megaNavigator }
}