package mega.privacy.android.app.presentation.audiosection

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.AudioNavKey
import mega.privacy.android.navigation.destination.AudioSectionNavKey

/**
 * Transparent gate for the Audio section: [FeatureFlagGate] on [ApiFeatures.AudioSectionRevamp]
 * either opens legacy [AudioSectionActivity] or navigates to [AudioNavKey] (Compose, cloud-drive).
 */
fun EntryProviderScope<NavKey>.audioSectionDestination(
    removeDestination: () -> Unit,
    navigationHandler: NavigationHandler,
) {
    entry<AudioSectionNavKey>(
        metadata = transparentMetadata(),
    ) {
        AudioSectionEntry(
            removeDestination = removeDestination,
            navigationHandler = navigationHandler,
        )
    }
}

@Composable
private fun AudioSectionEntry(
    removeDestination: () -> Unit,
    navigationHandler: NavigationHandler,
) {
    val context = LocalContext.current
    FeatureFlagGate(
        feature = ApiFeatures.AudioSectionRevamp,
        disabled = {
            LaunchedEffect(Unit) {
                val intent = Intent(context, AudioSectionActivity::class.java)
                context.startActivity(intent)
                removeDestination()
            }
        },
    ) {
        LaunchedEffect(Unit) {
            removeDestination()
            navigationHandler.navigate(AudioNavKey)
        }
    }
}
