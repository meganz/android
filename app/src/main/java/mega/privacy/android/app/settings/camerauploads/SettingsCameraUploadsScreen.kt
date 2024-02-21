package mega.privacy.android.app.settings.camerauploads

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import mega.privacy.android.app.settings.camerauploads.navigation.SettingsCameraUploadsNavHostController
import mega.privacy.android.core.ui.controls.layouts.MegaScaffold

/**
 * A Composable holding all Settings Camera Uploads screens using the Navigation Controller
 */
@Composable
internal fun SettingsCameraUploadsScreen() {
    val navHostController = rememberNavController()

    MegaScaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberScaffoldState(),
    ) { padding ->
        SettingsCameraUploadsNavHostController(
            modifier = Modifier.padding(padding),
            navHostController = navHostController,
        )
    }
}