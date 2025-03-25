package mega.privacy.android.app.presentation.settings

import android.os.Parcelable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.view.QASettingsHomeView

@Serializable
@Parcelize
internal object QASettingsHome : Parcelable

internal fun NavGraphBuilder.qaSettingsHomeDestination(navHostController: NavHostController) {

    composable<QASettingsHome> {
        QASettingsHomeView()
    }
}