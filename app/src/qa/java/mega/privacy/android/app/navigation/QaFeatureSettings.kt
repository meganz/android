package mega.privacy.android.app.navigation

import android.content.Context
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.QASettingsHome
import mega.privacy.android.app.presentation.settings.qaEntryPoints
import mega.privacy.android.navigation.settings.FeatureSettings

internal class QaFeatureSettings(@ApplicationContext private val context: Context) :
    FeatureSettings {
    override val entryPoints = qaEntryPoints(context)
    override val settingsNavGraph = NavGraphBuilder::qaSettingsNavigationGraph

    override fun getTitleForDestination(entry: NavBackStackEntry) =
        entry.destination.route?.let {
            if (it == QASettingsHome::class.qualifiedName) context.getString(R.string.settings_qa) else null
        }
}