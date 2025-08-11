package mega.privacy.android.navigation.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import mega.privacy.android.navigation.MegaActivityResultContract
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.megaActivityResultContract
import mega.privacy.android.navigation.megaNavigator

@Composable
fun rememberMegaNavigator(): MegaNavigator {
    val context = LocalContext.current
    return remember { context.megaNavigator }
}

@Composable
fun rememberMegaResultContract(): MegaActivityResultContract {
    val context = LocalContext.current
    return remember { context.megaActivityResultContract }
}