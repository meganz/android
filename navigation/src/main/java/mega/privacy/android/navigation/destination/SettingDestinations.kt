package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.settings.arguments.TargetPreference

@Serializable
data class LegacySettings(
    val targetPreference: TargetPreference?,
) : NavKey