package mega.privacy.android.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.QASettingsHome
import mega.privacy.android.app.presentation.settings.qaSettingsHomeDestination

@Serializable
internal object QASettingsGraph

internal fun NavGraphBuilder.qaSettingsNavigationGraph(
    navHostController: NavHostController,
) {
    navigation<QASettingsGraph>(
        startDestination = QASettingsHome,
    ) {
        qaSettingsHomeDestination()
    }
}

@Composable
internal fun getTitleForEntry(navBackStackEntry: NavBackStackEntry): String? {
    val route = navBackStackEntry.destination.route ?: return null
    return if (route == QASettingsHome::class.qualifiedName) stringResource(R.string.settings_qa) else null
}
