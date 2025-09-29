package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.settings.arguments.TargetPreference

@Serializable
data class LegacySettingsNavKey(
    val targetPreference: TargetPreference?,
) : NavKey

@Serializable
data object CookieSettingsNavKey : NavKey