package mega.privacy.android.feature.chat.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import kotlinx.serialization.Serializable

@Serializable
internal object CallSettingsGraph

internal fun NavGraphBuilder.callSettingsNavigationGraph(onBackPressed: () -> Unit) {
    navigation<CallSettingsGraph>(
        startDestination = CallSettingsHome,
    ) {
        callSettingsHome(onBackPressed = onBackPressed)
    }
}