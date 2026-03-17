package mega.privacy.android.app.presentation.audiosection

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyAudioSectionNavKey

fun EntryProviderScope<NavKey>.audioSectionLegacyDestination(
    removeDestination: () -> Unit,
) {
    entry<LegacyAudioSectionNavKey>(
        metadata = transparentMetadata()
    ) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, AudioSectionActivity::class.java)
            context.startActivity(intent)

            removeDestination()
        }

    }
}