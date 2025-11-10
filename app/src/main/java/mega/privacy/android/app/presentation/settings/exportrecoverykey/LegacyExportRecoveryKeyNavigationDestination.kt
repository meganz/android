package mega.privacy.android.app.presentation.settings.exportrecoverykey

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyExportRecoveryKeyNavKey

fun EntryProviderScope<NavKey>.legacyExportRecoveryKeyScreen(removeDestination: () -> Unit) {
    entry<LegacyExportRecoveryKeyNavKey>(
        metadata = transparentMetadata()
    ) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = ExportRecoveryKeyActivity.getIntent(context)
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

