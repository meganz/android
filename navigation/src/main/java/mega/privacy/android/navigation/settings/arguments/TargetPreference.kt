package mega.privacy.android.navigation.settings.arguments

import kotlinx.serialization.Serializable

/**
 * Target preference
 *
 * @property preferenceId
 * @property requiresNavigation
 */
@Serializable
data class TargetPreference(
    val preferenceId: String,
    val requiresNavigation: Boolean,
    val rootKey: String,
)