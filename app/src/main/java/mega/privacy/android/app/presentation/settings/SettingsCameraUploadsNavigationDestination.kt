package mega.privacy.android.app.presentation.settings

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
import mega.privacy.android.navigation.megaNavigator

fun EntryProviderScope<NavKey>.settingsCameraUploadsNavigationDestination(removeDestination: () -> Unit) {
    entry<SettingsCameraUploadsNavKey>(
        metadata = transparentMetadata()
    ) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.megaNavigator.openSettingsCameraUploads(context)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

