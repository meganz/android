package mega.privacy.android.navigation.destination

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.settings.arguments.TargetPreference

@Serializable
data class LegacySettingsNavKey(
    val targetPreference: TargetPreference?,
) : NavKey

/**
 * Preference key of the switch that auto-accepts contact requests received through the user's
 * QR code, shared by the settings screen that hosts it and the screens that navigate to it.
 */
const val QR_CODE_AUTO_ACCEPT_PREFERENCE_KEY = "settings_qrcode_autoaccept"

/**
 * Settings target that opens the security section at the QR code auto-accept switch.
 */
val qrCodeSettingsTargetPreference = TargetPreference(
    preferenceId = QR_CODE_AUTO_ACCEPT_PREFERENCE_KEY,
    requiresNavigation = false,
    rootKey = "security",
)

@Serializable
data object CookieSettingsNavKey : NavKey

@Serializable
@Parcelize
data object SettingsCameraUploadsNavKey : NavKey, Parcelable

@Serializable
@Parcelize
data object SyncSettingsNavKey : NavKey, Parcelable

@Serializable
data object TransfersSettingsNavKey : NavKey

@Serializable
data object SortingAndViewModeSettingsNavKey : NavKey