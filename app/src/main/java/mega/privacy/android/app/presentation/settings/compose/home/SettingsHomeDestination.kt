package mega.privacy.android.app.presentation.settings.compose.home

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.compose.home.view.SettingsHomeView

/**
 * Settings home
 *
 * @property initialSetting
 */
@Serializable
data class SettingsHome(
    val initialSetting: Pair<String, Parcelable?>?,
)

/**
 * Settings home
 *
 */
fun NavGraphBuilder.settingsHome(
    onBackPressed: () -> Unit,
) {
    composable<SettingsHome> { backStackEntry ->
        val args = backStackEntry.toRoute<SettingsHome>()
        DestinationContent(args = args, onBackPressed = onBackPressed)
    }
}

@Composable
private fun DestinationContent(
    args: SettingsHome,
    onBackPressed: () -> Unit,
) {
    val viewModel = hiltViewModel<SettingHomeViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val initialNavData = args.initialSetting?.first?.let { initialKey ->
        (state.featureEntryPoints + state.moreEntryPoints).map { it.navData }
            .find { it.key == initialKey }
    }
    SettingsHomeView(
        state = state,
        onBackPressed = onBackPressed,
        initialScreen = initialNavData,
    )
}

/**
 * This is used while we do not have a global graph to which we can add the settings home destination above
 */
@Composable
fun SettingsHomeDestinationWrapper(
    onBackPressed: () -> Unit,
    initialSetting: Pair<String, Parcelable?>?,
) {
    DestinationContent(
        args = SettingsHome(initialSetting = initialSetting),
        onBackPressed = onBackPressed,
    )
}