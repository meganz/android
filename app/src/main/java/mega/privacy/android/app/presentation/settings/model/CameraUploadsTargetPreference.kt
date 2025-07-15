package mega.privacy.android.app.presentation.settings.model

import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.navigation.settings.arguments.TargetPreference

/**
 * Start screen
 */
data object CameraUploadsTargetPreference : TargetPreference {
    override val preferenceId = SettingsConstants.KEY_FEATURES_CAMERA_UPLOAD
    override val requiresNavigation = true
    override val rootKey = "camera_uploads"
}