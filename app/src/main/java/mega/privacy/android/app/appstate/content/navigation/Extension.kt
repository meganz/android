package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.destination.HomeScreensNavKey

fun NavKey.isHomeScreenKey() = this is HomeScreensNavKey