package mega.privacy.android.app.presentation.settings.model

import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.navigation.settings.arguments.TargetPreference

/**
 * Camera uploads
 */
val cameraUploadsTargetPreference = TargetPreference(
    preferenceId = SettingsConstants.KEY_FEATURES_CAMERA_UPLOAD,
    requiresNavigation = true,
    rootKey = "camera_uploads"
)